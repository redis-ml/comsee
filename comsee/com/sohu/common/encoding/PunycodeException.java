package com.sohu.common.encoding;

class PunycodeException
  extends Exception
{
  public static String OVERFLOW = "Overflow.";
  public static String BAD_INPUT = "Bad input.";

  /**
   * Creates a new PunycodeException.
   *
   * @param m message.
   */
  public PunycodeException(String m)
  {
    super(m);
  }
}