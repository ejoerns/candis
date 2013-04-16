package candis.example.hash;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Enrico Joerns
 */
class BruteForceStringGenerator {

  private final List<Integer> mValues = new LinkedList<Integer>();
  private char[] mAlphabet;
  boolean first = true;
  private final BruteForceScheduler mStringGenerator;

  public BruteForceStringGenerator(int initSize, char[] alphabet, final BruteForceScheduler mStringGenerator) {
    this.mStringGenerator = mStringGenerator;
    mAlphabet = alphabet;
    if (initSize < 0) {
      initSize = 0;
    }
    // init with first letter of alphabet
    for (int i = 0; i < initSize; i++) {
      mValues.add(0);
    }
  }

  @Override
  public String toString() {
    String str = "";
    for (Integer c : mValues) {
      str = mAlphabet[c] + str;
    }
    return str;
  }

  public String nextString() {
    if (first) {
      first = false;
    }
    else {
      inc(0);
    }
    return toString();
  }

  /**
   * Recursive calculation of next string.
   *
   * @param pos pos of letter to increment
   */
  private void inc(int pos) {
    // add letter if pos exceeds current string size
    if (pos >= mValues.size()) {
      mValues.add(0);
    }
    else {
      int i = mValues.get(pos) + 1;
      // if iteration over alphabet done for current letter,
      // recursive call for next pos
      if (i >= mAlphabet.length) {
        i = 0;
        inc(pos + 1);
      }
      mValues.set(pos, i);
    }
  }
}
