package com.github.longboyy.repelshitters;

/**
 * Snapshot of the live damage settings, see {@link RepelShittersConfig#getDamageConfig()}
 */
public record DamageConfig(double startingScalar, long gameTimeThreshold, long firstJoinThreshold, double gameTimeWeight, double firstTimeWeight) {
}
