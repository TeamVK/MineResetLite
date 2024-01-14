package com.koletar.jj.mineresetlite;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;


/**
 * @author jjkoletar
 * @author vk2gpz
 */
public class StringTools {
	/**
	 * Build a spaced argument out of an array of args that don't contain spaces, due to the command delimiter.
	 *
	 * @param args  String array of args
	 * @param start Number of elements to skip over/index to begin at
	 * @param stop  Number of elements at the <b>end of the array</b> to ignore, <u>not</u> a stopping index.
	 * @return Reconstructed spaced argument
	 */
	private static @NotNull String buildSpacedArgument(@NotNull String[] args, int start, int stop) {
		int end = (stop <= 0) ? args.length : Math.min(args.length, stop);
		return start >= end ? "" : String.join(" ", Arrays.copyOfRange(args, start, end));
	}
	
	public static @NotNull String buildSpacedArgument(String[] args, int stop) {
		return buildSpacedArgument(args, 0, stop);
	}
	
	public static @NotNull String buildSpacedArgument(String[] args) {
		return buildSpacedArgument(args, 0);
	}
	
	private static String buildList(Object[] items, String prefix, String suffix) {
		StringJoiner sj = new StringJoiner(suffix);
		
		Arrays.stream(items)
				.map(item -> prefix + Phrases.findName(item))
				.forEach(sj::add);
		
		return sj.toString();
	}
	
	public static String buildList(List<?> items, String prefix, String suffix) {
		return buildList(items.toArray(), prefix, suffix);
	}
}
