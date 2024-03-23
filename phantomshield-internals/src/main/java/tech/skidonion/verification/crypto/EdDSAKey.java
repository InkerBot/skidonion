/**
 * EdDSA-Java by str4d
 * <p>
 * To the extent possible under law, the person who associated CC0 with
 * EdDSA-Java has waived all copyright and related or neighboring rights
 * to EdDSA-Java.
 * <p>
 * You should have received a copy of the CC0 legalcode along with this
 * work. If not, see <https://creativecommons.org/publicdomain/zero/1.0/>.
 */
package tech.skidonion.verification.crypto;

import tech.skidonion.verification.crypto.spec.EdDSAParameterSpec;

/**
 * Common interface for all EdDSA keys.
 * @author str4d
 */
public interface EdDSAKey {

    EdDSAParameterSpec getParams();
}
