package net.typho.data_util.anno

/**
 * Specify to reflected Codecs that the class (assuming it has exactly one non-optional field) could have an inlined codec.
 *
 * For example, this class:
 * ```java
 * @InlineCodec
 * public record Example(
 *     String primary,
 *     @Nullable String fallback
 * )
 * ```
 * would generate a codec that would be valid for these inputs:
 * ```jsonc
 * [
 *     "abc", // Example("abc", null)
 *     {
 *         "primary": "def" // Example("def", null)
 *     },
 *     {
 *         "primary": "ghi" // Example("ghi", "123"),
 *         "fallback": 123
 *     }
 * ]
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class InlineCodec