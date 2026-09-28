package com.tabletportals;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.ActorSpotAnim;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Renderable;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replaces the plain teleport-tablet animation with a proper send-off: your character smashes the tablet, a
 * portal tears open in front of them with smoke rolling around it, and they walk into it. At the other end a
 * portal opens and they step out. Everything is drawn on your screen only; the teleport itself and its timing
 * are the game's, untouched.
 */
@PluginDescriptor(
	name = "Tablet Portals",
	internalName = "tablet-portals",
	description = "Smash the tablet, a smoky portal opens, and you walk in. Replaces the teleport tablet animation.",
	tags = {"teleport", "tablet", "tab", "portal", "animation", "cosmetic", "fun"}
)
public class TabletPortalsPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(TabletPortalsPlugin.class);

	/** The game's tablet-break animations (the older one and its newer variants). */
	static final Set<Integer> TABLET_BREAK = Set.of(
		AnimationID.POH_SMASH_MAGIC_TABLET,
		AnimationID.POH_SMASH_MAGIC_TABLET_WALKMERGE,
		AnimationID.POH_SMASH_MAGIC_TABLET_PRIORITY);
	/** Every animation the game plays for a tablet teleport, which we hold off while ours runs. */
	static final Set<Integer> TABLET_ANIMATIONS = Set.of(
		AnimationID.POH_SMASH_MAGIC_TABLET,
		AnimationID.POH_SMASH_MAGIC_TABLET_WALKMERGE,
		AnimationID.POH_SMASH_MAGIC_TABLET_PRIORITY,
		AnimationID.POH_ABSORB_TABLET_TELEPORT);
	/** The game's blue tablet sparkle, which our effects replace. */
	static final int TABLET_SPARKLE = SpotanimID.POH_ABSORB_TABLET_MAGIC;

	private static final int SMOKE = SpotanimID.SMOKEPUFF_LARGE;
	private static final int SMALL_SMOKE = SpotanimID.SMALL_SMOKEPUFF;
	private static final int SHATTER = SpotanimID.SP_ATTACK_SHATTER_SPOTANIM;
	/** Tablets are soft-fired clay; the shards take this colour. */
	private static final Color CLAY = new Color(185, 145, 100);
	/** The flash as you step through a classic purple portal. */
	private static final Color CLASSIC_GLOW = new Color(150, 70, 210);

	/** How many game ticks after clicking "Break" the smash animation can still start. */
	private static final int CLICK_WINDOW = 3;
	/** Client ticks after arriving before the destination is photographed (once the exit portal and smoke are gone). */
	private static final int PHOTO_DELAY = 110;
	/** Moving further than this in one go (tiles) means the teleport has happened. */
	private static final int ARRIVED_DISTANCE = 12;

	/** Where smoke puffs go around the portal: {sideways, forwards} from its middle, in local units. */
	private static final int[][] SMOKE_SPOTS = {{-85, 0}, {85, 0}, {0, -35}, {-55, 35}, {55, 35}, {0, 45}};

	enum Phase
	{
		IDLE,
		/** Smash, portal opens, walk in, portal closes. */
		DEPARTING,
		/** Gone through the portal; hidden until the game moves us. */
		WAITING,
		/** Arrived: a portal opens and we walk out of it. */
		ARRIVING,
	}

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private RenderCallbackManager renderCallbackManager;

	@Inject
	private TabletPortalsConfig config;

	@Inject
	private Effects effects;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ReflectionOverlay reflection;

	@Inject
	private Snapshots snapshots;

	private final Poser poser = new Poser();

	private Phase phase = Phase.IDLE;
	/** Client ticks since the tablet was broken (departing, waiting) or since arriving. */
	private int t;
	private Timeline timeline;
	private SmashStyle style;
	/** Game tick of the last "Break" on a teleport tablet. */
	private int breakClickTick = Integer.MIN_VALUE / 2;

	/** Hide the real player (a walking copy is drawn instead, or they're "inside" the portal). */
	private boolean hidden;
	private Walker walker;
	private Portal portal;

	private LocalPoint origin;
	private LocalPoint portalAt;
	private int plane;
	private int orientation;
	private WorldPoint startWorld;
	/** Where the player stands after arriving; the walk out of the exit portal ends here. */
	private LocalPoint arrivedAt;
	/** Tick (in {@link #t}) the exit portal starts closing, or -1. */
	private int exitClosingAt = -1;

	/** The tablet being used, as a key for its destination's picture. */
	private String tabletKey;
	/** The view where you broke the tablet, shown in the exit portal. */
	private BufferedImage originShot;
	/** Client ticks until the destination is photographed, or -1; and which tablet it's for. */
	private int photoIn = -1;
	private String photoKey;

	private Color portalTint;
	private Color smokeTint;
	private Color glow;

	private final RenderCallback renderCallback = new RenderCallback()
	{
		@Override
		public boolean addEntity(Renderable renderable, boolean ui)
		{
			// Keep overhead text and hitsplats (drawn as UI); hide only the player's body.
			return ui || !hidden || renderable != client.getLocalPlayer();
		}
	};

	@Provides
	TabletPortalsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TabletPortalsConfig.class);
	}

	@Override
	protected void startUp()
	{
		renderCallbackManager.register(renderCallback);
		overlayManager.add(reflection);
		try
		{
			snapshots.setDirectory(getPluginDirectory());
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("No data folder; destination pictures will only last until logout", e);
			snapshots.setDirectory(null);
		}
	}

	@Override
	protected void shutDown()
	{
		renderCallbackManager.unregister(renderCallback);
		overlayManager.remove(reflection);
		clientThread.invoke(() ->
		{
			finish(client.getLocalPlayer());
			effects.clear();
		});
	}

	// ------------------------------------------------------------------
	// Starting
	// ------------------------------------------------------------------

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked e)
	{
		if (isTeleportTabletBreak(e.getMenuOption(), e.getMenuTarget()) && phase == Phase.IDLE)
		{
			breakClickTick = client.getTickCount();
			tabletKey = Snapshots.key(e.getMenuTarget());
			// A picture of here, for the exit portal to show where you came from.
			originShot = null;
			if (config.reflection() && config.exitPortal())
			{
				snapshots.grab(img -> originShot = img);
			}
		}
	}

	/** "Break" on a teleport tablet (every one has "teleport" in its name), not on other magic tablets. */
	static boolean isTeleportTabletBreak(String option, String target)
	{
		if (option == null || target == null || !option.equalsIgnoreCase("Break"))
		{
			return false;
		}
		String name = target.replaceAll("<[^>]*>", "").toLowerCase();
		return name.contains("teleport");
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged e)
	{
		Player me = client.getLocalPlayer();
		if (me == null || e.getActor() != me || phase != Phase.IDLE)
		{
			return;
		}
		if (!TABLET_BREAK.contains(me.getAnimation()))
		{
			return;
		}
		if (client.getTickCount() - breakClickTick > CLICK_WINDOW)
		{
			return; // a magic tablet that doesn't teleport (enchanting, bones to bananas...)
		}
		breakClickTick = Integer.MIN_VALUE / 2;
		start(me);
	}

	private void start(Player me)
	{
		LocalPoint here = me.getLocalLocation();
		if (here == null)
		{
			return;
		}
		style = config.smashStyle();
		timeline = new Timeline(style);
		origin = here;
		plane = me.getWorldView().getPlane();
		orientation = me.getCurrentOrientation();
		startWorld = me.getWorldLocation();
		portalAt = offset(origin, Facing.ahead(orientation, Timeline.PORTAL_DISTANCE));

		portalTint = config.portalColour().resolve(config.customColour());
		glow = portalTint != null ? portalTint : CLASSIC_GLOW;
		smokeTint = config.tintSmoke() ? glow : null;

		portal = Portal.build(client, portalTint, config.portalSize());
		walker = new Walker(client, me, this::holdWalk);
		if (portal != null)
		{
			portal.place(portalAt, plane, orientation);
			if (config.reflection())
			{
				reflection.show(portal, portalAt, plane, orientation, snapshots.get(tabletKey), walker);
			}
		}

		t = 0;
		exitClosingAt = -1;
		phase = Phase.DEPARTING;

		me.setAnimation(style.getAnimation());
		me.setAnimationFrame(0);
		removeSparkle(me);
	}

	// ------------------------------------------------------------------
	// Every client tick
	// ------------------------------------------------------------------

	@Subscribe
	public void onClientTick(ClientTick e)
	{
		effects.tick();
		if (photoIn >= 0 && --photoIn < 0 && phase == Phase.IDLE && client.getGameState() == GameState.LOGGED_IN)
		{
			snapshots.capture(photoKey);
		}
		if (phase == Phase.IDLE)
		{
			return;
		}
		Player me = client.getLocalPlayer();
		if (me == null)
		{
			reset();
			return;
		}
		removeSparkle(me);

		if ((phase == Phase.DEPARTING || phase == Phase.WAITING) && hasTeleported(me))
		{
			arrive(me);
			return;
		}

		t++;
		switch (phase)
		{
			case DEPARTING:
				depart(me);
				break;
			case WAITING:
				holdWalk(me);
				if (t > Timeline.GIVE_UP)
				{
					// The teleport never came (blocked, or interrupted): step back out where we are.
					effects.play(SMOKE, me.getLocalLocation(), plane, 0, orientation, smokeTint);
					finish(me);
				}
				break;
			case ARRIVING:
				exit(me);
				break;
			default:
				break;
		}
	}

	private void depart(Player me)
	{
		Timeline tl = timeline;

		// Keep our smash showing over the game's own tablet animation.
		if (t < style.getLength() && me.getAnimation() != style.getAnimation()
			&& TABLET_ANIMATIONS.contains(me.getAnimation()))
		{
			me.setAnimation(style.getAnimation());
			me.setAnimationFrame(0);
		}
		else if (t >= style.getLength() && TABLET_ANIMATIONS.contains(me.getAnimation()))
		{
			me.setAnimation(-1); // the game's "absorb" pose; we're about to walk into the portal instead
		}

		if (t == tl.impact && config.shatter())
		{
			LocalPoint ground = offset(origin, Facing.ahead(orientation, 40));
			effects.play(SHATTER, ground, plane, 10, orientation, CLAY);
			effects.play(SMALL_SMOKE, ground, plane, 0, orientation, null);
		}

		if (portal == null)
		{
			// Couldn't build the portal (models not loaded): just the smash, then let the game carry on.
			if (t >= style.getLength())
			{
				finish(me);
			}
			return;
		}

		if (t >= tl.portalStart && t < tl.enter)
		{
			portal.setSize(Timeline.opening(t - tl.portalStart, Timeline.PORTAL_OPEN));
		}
		for (int i = 0; i < tl.smoke.length; i++)
		{
			if (t == tl.smoke[i])
			{
				smoke(i, tl.smoke.length);
			}
		}

		if (t == tl.walkStart)
		{
			me.setAnimation(-1);
			poser.apply(me);
			hidden = true;
			walker.walk(origin, portalAt, plane, orientation);
		}
		if (t > tl.walkStart && t < tl.enter)
		{
			walker.moveTo(Timeline.progress(t - tl.walkStart, Timeline.WALK));
		}
		if (t == tl.enter)
		{
			walker.remove();
			effects.play(SMALL_SMOKE, portalAt, plane, 110, orientation, glow);
			if (config.smoke() != SmokeAmount.NONE)
			{
				effects.play(SMOKE, portalAt, plane, 0, orientation, smokeTint);
			}
		}
		if (t > tl.enter)
		{
			portal.setSize(Timeline.closing(t - tl.enter, Timeline.PORTAL_CLOSE));
			if (t >= tl.closed)
			{
				portal.remove();
				phase = Phase.WAITING;
			}
		}
	}

	/** Puffs of smoke around the portal for beat {@code beat} of {@code beats}. */
	private void smoke(int beat, int beats)
	{
		int puffs = config.smoke().getPuffs();
		if (puffs == 0)
		{
			return;
		}
		// Share the puffs out over the beats: 3 puffs is one per beat, 5 is two, two, one.
		int from = beat * puffs / beats;
		int to = (beat + 1) * puffs / beats;
		for (int i = from; i < to; i++)
		{
			int[] spot = SMOKE_SPOTS[i % SMOKE_SPOTS.length];
			int[] side = Facing.right(orientation, spot[0]);
			int[] fwd = Facing.ahead(orientation, spot[1]);
			LocalPoint at = offset(portalAt, new int[]{side[0] + fwd[0], side[1] + fwd[1]});
			effects.play(SMOKE, at, plane, 0, orientation, smokeTint);
		}
	}

	private boolean hasTeleported(Player me)
	{
		WorldPoint now = me.getWorldLocation();
		if (now == null || startWorld == null)
		{
			return false;
		}
		return now.getPlane() != startWorld.getPlane() || now.distanceTo2D(startWorld) > ARRIVED_DISTANCE;
	}

	// ------------------------------------------------------------------
	// Arriving
	// ------------------------------------------------------------------

	private void arrive(Player me)
	{
		// Whatever was left at the old spot is gone with the old scene.
		reflection.hide();
		if (config.reflection() && snapshots.wants(tabletKey))
		{
			photoKey = tabletKey;
			photoIn = PHOTO_DELAY;
		}
		if (walker != null)
		{
			walker.remove();
		}
		if (portal != null)
		{
			portal.remove();
		}
		effects.clear();

		LocalPoint here = me.getLocalLocation();
		if (!config.exitPortal() || here == null)
		{
			if (here != null && config.smoke() != SmokeAmount.NONE)
			{
				effects.play(SMOKE, here, me.getWorldView().getPlane(), 0, orientation, smokeTint);
			}
			finish(me);
			return;
		}

		plane = me.getWorldView().getPlane();
		orientation = me.getCurrentOrientation();
		arrivedAt = here;
		// The exit portal opens behind you and you walk forwards out of it, onto your tile.
		portalAt = offset(here, Facing.ahead(orientation, -Timeline.PORTAL_DISTANCE));
		portal = Portal.build(client, portalTint, config.portalSize());
		if (portal == null)
		{
			finish(me);
			return;
		}
		portal.place(portalAt, plane, orientation);
		walker = new Walker(client, me, this::holdWalk);
		if (config.reflection() && originShot != null)
		{
			reflection.show(portal, portalAt, plane, orientation, originShot, walker);
		}

		stopTabletAnimation(me);
		poser.apply(me);
		hidden = true;
		t = 0;
		exitClosingAt = -1;
		phase = Phase.ARRIVING;

		if (config.smoke() != SmokeAmount.NONE)
		{
			effects.play(SMOKE, portalAt, plane, 0, orientation, smokeTint);
		}
	}

	private void exit(Player me)
	{
		int walkStart = Timeline.EXIT_OPEN;
		int walkEnd = walkStart + Timeline.EXIT_WALK;

		if (exitClosingAt < 0)
		{
			portal.setSize(Timeline.opening(t, Timeline.EXIT_OPEN));

			if (t == walkStart)
			{
				walker.walk(portalAt, arrivedAt, plane, orientation);
				effects.play(SMALL_SMOKE, portalAt, plane, 110, orientation, glow);
			}
			else if (t > walkStart)
			{
				walker.moveTo(Timeline.progress(t - walkStart, Timeline.EXIT_WALK));
			}

			// Out of the portal, or cut short because you've started moving or doing something.
			boolean moved = !arrivedAt.equals(me.getLocalLocation());
			boolean busy = me.getAnimation() != -1 && !TABLET_ANIMATIONS.contains(me.getAnimation());
			if (t >= walkEnd || moved || busy)
			{
				showPlayer(me);
				exitClosingAt = t;
				if (config.smoke() != SmokeAmount.NONE)
				{
					effects.play(SMOKE, portalAt, plane, 0, orientation, smokeTint);
				}
			}
			return;
		}

		int closing = t - exitClosingAt;
		portal.setSize(Timeline.closing(closing, Timeline.PORTAL_CLOSE));
		if (closing >= Timeline.PORTAL_CLOSE)
		{
			finish(me);
		}
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------

	/** Keeps the (hidden) player walking on the spot, for the walking copy. Runs every frame it's drawn. */
	private void holdWalk(Player me)
	{
		if (!hidden)
		{
			return;
		}
		stopTabletAnimation(me);
		poser.hold(me);
	}

	private void stopTabletAnimation(Player me)
	{
		if (TABLET_ANIMATIONS.contains(me.getAnimation()) || (style != null && me.getAnimation() == style.getAnimation()))
		{
			me.setAnimation(-1);
		}
	}

	private void showPlayer(Player me)
	{
		if (walker != null)
		{
			walker.remove();
		}
		if (hidden)
		{
			hidden = false;
			poser.restore(me);
		}
	}

	/** Ends everything and shows the player normally. */
	private void finish(Player me)
	{
		reflection.hide();
		originShot = null;
		showPlayer(me);
		if (portal != null)
		{
			portal.remove();
		}
		portal = null;
		walker = null;
		phase = Phase.IDLE;
		if (me != null && style != null && me.getAnimation() == style.getAnimation())
		{
			me.setAnimation(-1);
		}
	}

	/** Ends everything without touching the player (they're gone: logged out, hopping). */
	private void reset()
	{
		reflection.hide();
		originShot = null;
		photoIn = -1;
		if (walker != null)
		{
			walker.remove();
		}
		if (portal != null)
		{
			portal.remove();
		}
		portal = null;
		walker = null;
		hidden = false;
		poser.forget();
		effects.clear();
		phase = Phase.IDLE;
	}

	/** Takes the game's blue tablet sparkle off the player; ours replaces it. */
	private static void removeSparkle(Player me)
	{
		if (!me.hasSpotAnim(TABLET_SPARKLE))
		{
			return;
		}
		List<Integer> keys = new ArrayList<>();
		for (ActorSpotAnim sa : me.getSpotAnims())
		{
			if (sa.getId() == TABLET_SPARKLE)
			{
				keys.add((int) sa.getHash());
			}
		}
		for (int key : keys)
		{
			me.removeSpotAnim(key);
		}
	}

	private static LocalPoint offset(LocalPoint p, int[] d)
	{
		return new LocalPoint(p.getX() + d[0], p.getY() + d[1], p.getWorldView());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		GameState s = e.getGameState();
		if (s == GameState.LOGIN_SCREEN || s == GameState.HOPPING || s == GameState.CONNECTION_LOST)
		{
			reset();
		}
		if (s == GameState.LOGIN_SCREEN)
		{
			snapshots.newSession();
		}
	}

	Phase getPhase()
	{
		return phase;
	}
}
