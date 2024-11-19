package com.goldsprite.util;

public class Vector2 {
	public float x, y;
	public Vector2(){this(0, 0);}
	public Vector2(Vector2 vec){this(vec.x, vec.y);}
	public Vector2(float px, float py){x = px;y = py;}
	public String toString(){return String.format("{%.1f, %.1f}", x, y); }
}
