package com.github.longboyy.repelshitters;

/**
 * Snapshot of the live reinforcement damage settings, see {@link RepelShittersConfig#getCitadelConfig()}
 */
public record CitadelConfig(double startingScalar, long gameTimeThreshold, long firstJoinThreshold, double gameTimeWeight, double firstTimeWeight) {
}
