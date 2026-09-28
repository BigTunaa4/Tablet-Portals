package com.tabletportals;

/** How choppy the water-like reflection in the portal is. */
public enum Ripple
{
	CALM("Calm", 0.010f, 1.0f),
	CHOPPY("A little choppy", 0.022f, 1.5f),
	ROUGH("Rough", 0.040f, 2.1f);

	private final String label;
	/** How far rows sway sideways, as a fraction of the reflection's height. */
	private final float amplitude;
	/** How fast the ripples move. */
	private final float speed;

	Ripple(String label, float amplitude, float speed)
	{
		this.label = label;
		this.amplitude = amplitude;
		this.speed = speed;
	}

	float getAmplitude()
	{
		return amplitude;
	}

	float getSpeed()
	{
		return speed;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
