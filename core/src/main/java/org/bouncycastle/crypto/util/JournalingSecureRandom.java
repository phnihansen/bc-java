package org.bouncycastle.crypto.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

import org.bouncycastle.util.Arrays;

/**
* A SecureRandom that maintains a journal of its output.
* This can be used to recreate an output that was based on randomness
* For the transcript to be reusable, the order of the requests for randomness during recreation must be consistent with the initial requests and no other sources of randomness may be used.
 */
public class JournalingSecureRandom
    extends SecureRandom
{
    private static byte[] EMPTY_TRANSCRIPT = new byte[0];

    private final SecureRandom base;
    private final byte[] transcript;

    private TranscriptStream tOut = new TranscriptStream();
    private int index = 0;
    
    /**
     * Default constructor that takes an arbitrary SecureRandom as initial random
     */
    public JournalingSecureRandom() {
    	
    	this(new SecureRandom());
    
    }

    /**
     * Base constructor - no prior transcript.
     *
     * @param random source of randomness we will be journaling.
     */
    public JournalingSecureRandom(SecureRandom random)
    {
        this.base = random;
        this.transcript = EMPTY_TRANSCRIPT;
    }

    /**
     * Constructor with a prior transcript. Both the transcript used and
     * any new randomness are journaled.
     *
     * @param transcript initial transcript of randomness.
     * @param random source of randomness we will be journaling when the transcript runs out.
     */
    public JournalingSecureRandom(byte[] transcript, SecureRandom random)
    {
         this.base = random;
         this.transcript = Arrays.clone(transcript);
    }

    /**
     * Fill bytes with random data, journaling the random data before returning or re-use previously used random data, depending on the state of the transcript.
     *
     * @param bytes a block of bytes to be filled with random data.
     */
    public final void nextBytes(byte[] bytes)
    {
        if (index >= transcript.length)
        {
            base.nextBytes(bytes);
        }
        else
        {
            int i = 0;

            while (i != bytes.length)
            {
                if (index < transcript.length)
                {
                    bytes[i] = transcript[index++];
                }
                else
                {
                    break;
                }
                i++;
            }

            if (i != bytes.length)
            {
                byte[] extra = new byte[bytes.length - i];

                base.nextBytes(extra);

                System.arraycopy(extra, 0, bytes, i, extra.length);
            }
        }

        try
        {
            tOut.write(bytes);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("unable to record transcript: " + e.getMessage());
        }
    }

    /**
     * Clear the internals
     */
    public void clear()
    {
        Arrays.fill(transcript, (byte)0);
        tOut.clear();
    }
    
    /**
     * Resets the index to zero such that the randomness will now be reused
     */
    public void reset() {
    	
    	index = 0;
    	
    }

    /**
     * Return the transcript so far,
     *
     * @return a copy of the randomness produced so far.
     */
    public byte[] getTranscript()
    {
        return tOut.toByteArray();
    }

    private class TranscriptStream
        extends ByteArrayOutputStream
    {
        public void clear()
        {
            Arrays.fill(buf, (byte)0);
        }
    }
}
