package com.bbn.serif.theories;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.AbstractList;
import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class GornAddress extends AbstractList<Integer> {

  public GornAddress(Iterable<Integer> address) {
    this.address = ImmutableList.copyOf(address);
    checkArgument(!this.address.isEmpty());
    checkArgument(this.address.get(0) == 0, "First element of Gorn address must be 0");
  }

  private static final Pattern VALID_GORN_ADDRESS = Pattern.compile("0(.\\d+)*");
  private static final Splitter SPLIT_ON_DOTS = Splitter.on(".");


  public static GornAddress fromString(final String gornAddress) {
    checkNotNull(gornAddress);
    checkArgument(!gornAddress.isEmpty());
    checkArgument(VALID_GORN_ADDRESS.matcher(gornAddress).matches(),
        String.format("Invalid Gorn address: %s", gornAddress));

    List<Integer> ret = Lists.newArrayList();
    for (final String part : SPLIT_ON_DOTS.split(gornAddress)) {
      ret.add(Integer.valueOf(part));
    }
    return new GornAddress(ret);
  }

  private static Joiner dottedJoiner = Joiner.on(".");

  public String toDottedString() {
    return dottedJoiner.join(this);
  }

  private final List<Integer> address;


  @Override
  public Integer get(int idx) {
    return address.get(idx);
  }


  @Override
  public int size() {
    return address.size();
  }
}
