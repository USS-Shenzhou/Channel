package cn.ussshenzhou.channel.util;

import com.mojang.logging.LogUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.slf4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;

/**
 * The OpenALUtil class provides utility functions for working with OpenAL audio.
 */
public class OpenAlUtil {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Converts an OpenAL error code to a human-readable error message.
     * @return A String representing the error message for the given error code.
     *
     * @param error The OpenAL error code to convert
     */
    private static String alErrorToString(int error) {
        switch (error) {
            case 40961:
                return "Invalid name parameter.";
            case 40962:
                return "Invalid enumerated parameter value.";
            case 40963:
                return "Invalid parameter parameter value.";
            case 40964:
                return "Invalid operation.";
            case 40965:
                return "Unable to allocate memory.";
            default:
                return "An unrecognized error occurred.";
        }
    }

    /**
     * Checks for an OpenAL error and logs an error message if one is found.
     * @return true if an OpenAL error was found, false otherwise.
     *
     * @param location A String describing the operation being performed when the
     *                 error occurred
     */
    public static boolean checkALError(String location) {
        int error = AL10.alGetError();
        if (error != 0) {
            LOGGER.error("{}: {}", location, alErrorToString(error));
            return true;
        } else {
            return false;
        }
    }

    /**
     * Converts an ALC error code to a human-readable error message.
     * @return A String representing the error message for the given error code.
     *
     * @param error The ALC error code to convert
     */
    private static String alcErrorToString(int error) {
        switch (error) {
            case 40961:
                return "Invalid device.";
            case 40962:
                return "Invalid context.";
            case 40963:
                return "Illegal enum.";
            case 40964:
                return "Invalid value.";
            case 40965:
                return "Unable to allocate memory.";
            default:
                return "An unrecognized error occurred.";
        }
    }

    /**
     * Checks for an ALC error and logs an error message if one is found.
     * @return true if an ALC error was found, false otherwise.
     *
     * @param device   The handle of the device to check for errors on
     * @param location A String describing the operation being performed when the
     *                 error occurred
     */
    public static boolean checkALCError(long device, String location) {
        int error = ALC10.alcGetError(device);
        if (error != 0) {
            LOGGER.error("{} ({}): {}", location, device, alcErrorToString(error));
            return true;
        } else {
            return false;
        }
    }

    /**
     * Converts an AudioFormat object to the corresponding OpenAL audio format code.
     * @return An integer representing the corresponding OpenAL audio format code.
     * @throws IllegalArgumentException if the given AudioFormat is not a supported format.
     *
     * @param audioFormat The AudioFormat object to convert
     */
    static int audioFormatToOpenAl(AudioFormat audioFormat) {
        Encoding encoding = audioFormat.getEncoding();
        int channels = audioFormat.getChannels();
        int sampleSizeInBits = audioFormat.getSampleSizeInBits();
        if (encoding.equals(Encoding.PCM_UNSIGNED) || encoding.equals(Encoding.PCM_SIGNED)) {
            if (channels == 1) {
                if (sampleSizeInBits == 8) {
                    return 4352;
                }

                if (sampleSizeInBits == 16) {
                    return 4353;
                }
            } else if (channels == 2) {
                if (sampleSizeInBits == 8) {
                    return 4354;
                }

                if (sampleSizeInBits == 16) {
                    return 4355;
                }
            }
        }

        throw new IllegalArgumentException("Invalid audio format: " + audioFormat);
    }
}
