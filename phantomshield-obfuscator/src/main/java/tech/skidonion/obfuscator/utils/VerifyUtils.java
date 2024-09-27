package tech.skidonion.obfuscator.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;

public class VerifyUtils {
    public static JsonObject requestSoftwareInformation(String url, String uid, String token, String softwareId) {
        try {
            Map<String, String> headers = genericHeader(uid, token);
            Map<String, String> params = new HashMap<>();
            params.put("software_id", softwareId);
            return JsonParser.parseString(HttpUtils.post(url + "api/admin/software-information", params, headers)).getAsJsonObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Map<String, String> genericHeader(String uid, String token) {
        return new HashMap<String, String>() {
            {
                put("phantom-shield-x-uid", uid);
                put("phantom-shield-x-api-token", token);
            }
        };
    }

    // OID 1.3.101.xxx
    private static final int OID_OLD = 100;
    private static final int OID_ED25519 = 112;
    private static final int OID_BYTE = 8;
    private static final int IDLEN_BYTE = 3;

    public static byte[] decodePublicKey(byte[] d) {
        try {
            //
            // Setup and OID check
            //
            int totlen = 44;
            int idlen = 5;
            int doid = d[OID_BYTE];
            if (doid == OID_OLD) {
                totlen = 47;
                idlen = 8;
            } else if (doid == OID_ED25519) {
                // Detect parameter value of NULL
                if (d[IDLEN_BYTE] == 7) {
                    totlen = 46;
                    idlen = 7;
                }
            } else {
//                throw new InvalidKeySpecException("unsupported key spec");
                return new byte[0];
            }

            //
            // Pre-decoding check
            //
            if (d.length != totlen) {
//                throw new InvalidKeySpecException("invalid key spec length");
                return new byte[0];
            }

            //
            // Decoding
            //
            int idx = 0;
            if (d[idx++] != 0x30 ||
                    d[idx++] != (totlen - 2) ||
                    d[idx++] != 0x30 ||
                    d[idx++] != idlen ||
                    d[idx++] != 0x06 ||
                    d[idx++] != 3 ||
                    d[idx++] != (1 * 40) + 3 ||
                    d[idx++] != 101) {
//                throw new InvalidKeySpecException("unsupported key spec");
                return new byte[0];
            }
            idx++; // OID, checked above
            // parameters only with old OID
            if (doid == OID_OLD) {
                if (d[idx++] != 0x0a ||
                        d[idx++] != 1 ||
                        d[idx++] != 1) {
//                    throw new InvalidKeySpecException("unsupported key spec");
                    return new byte[0];
                }
            } else {
                // Handle parameter value of NULL
                //
                // Quoting RFC 8410 section 3:
                // > For all of the OIDs, the parameters MUST be absent.
                // >
                // > It is possible to find systems that require the parameters to be
                // > present. This can be due to either a defect in the original 1997
                // > syntax or a programming error where developers never got input where
                // > this was not true. The optimal solution is to fix these systems;
                // > where this is not possible, the problem needs to be restricted to
                // > that subsystem and not propagated to the Internet.
                //
                // Java's default keystore puts it in (when decoding as PKCS8 and then
                // re-encoding to pass on), so we must accept it.
                if (idlen == 7) {
                    if (d[idx++] != 0x05 ||
                            d[idx++] != 0) {
//                        throw new InvalidKeySpecException("unsupported key spec");
                        return new byte[0];
                    }
                }
            }
            if (d[idx++] != 0x03 ||
                    d[idx++] != 33 ||
                    d[idx++] != 0) {
//                throw new InvalidKeySpecException("unsupported key spec");
                return new byte[0];
            }
            byte[] rv = new byte[32];
            System.arraycopy(d, idx, rv, 0, 32);
            return rv;
        } catch (IndexOutOfBoundsException ioobe) {
//            throw new InvalidKeySpecException(ioobe);
            return new byte[0];
        }
    }
}
