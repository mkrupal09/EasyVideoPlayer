package com.dc.easyvideoplayer;

import android.net.Uri;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;

public final class EncryptedDataSource implements DataSource {

    private TransferListener mTransferListener;
    private StreamingCipherInputStream mInputStream;
    private Uri mUri;
    private long mBytesRemaining;
    private boolean mOpened;
    private Cipher mCipher;

    public EncryptedDataSource(Cipher cipher, TransferListener mTransferListener) {
        mCipher = cipher;
        mTransferListener = mTransferListener;
    }

    @Override
    public void addTransferListener(TransferListener transferListener) {

    }

    @Override
    public long open(DataSpec dataSpec) throws EncryptedFileDataSourceException {
        // if we're open, we shouldn't need to open again, fast-fail
        if (mOpened) {
            return mBytesRemaining;
        }
        // #getUri is part of the contract...
        mUri = dataSpec.uri;
        // put all our throwable work in a single block, wrap the error in a custom Exception
        try {
            setupInputStream();
            skipToPosition(dataSpec);
            computeBytesRemaining(dataSpec);
        } catch (IOException e) {
            throw new EncryptedFileDataSourceException(e);
        }
        // if we made it this far, we're open
        mOpened = true;
        // notify
        if (mTransferListener != null) {
            mTransferListener.onTransferStart(this, dataSpec, false);
        }
        // report
        return mBytesRemaining;
    }

    private void setupInputStream() throws FileNotFoundException {
        File encryptedFile = new File(mUri.getPath());
        FileInputStream fileInputStream = new FileInputStream(encryptedFile);
        mInputStream = new StreamingCipherInputStream(fileInputStream, mCipher);
    }

    private void skipToPosition(DataSpec dataSpec) throws IOException {
        mInputStream.forceSkip(dataSpec.position);
    }

    private void computeBytesRemaining(DataSpec dataSpec) throws IOException {
        if (dataSpec.length != C.LENGTH_UNSET) {
            mBytesRemaining = dataSpec.length;
        } else {
            mBytesRemaining = mInputStream.available();
            if (mBytesRemaining == Integer.MAX_VALUE) {
                mBytesRemaining = C.LENGTH_UNSET;
            }
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws EncryptedFileDataSourceException {
        // fast-fail if there's 0 quantity requested or we think we've already processed everything
        if (readLength == 0) {
            return 0;
        } else if (mBytesRemaining == 0) {
            return C.RESULT_END_OF_INPUT;
        }
        // constrain the read length and try to read from the cipher input stream
        int bytesToRead = getBytesToRead(readLength);
        int bytesRead;
        try {
            bytesRead = mInputStream.read(buffer, offset, bytesToRead);
        } catch (IOException e) {
            throw new EncryptedFileDataSourceException(e);
        }
        // if we get a -1 that means we failed to read - we're either going to EOF error or broadcast EOF
        if (bytesRead == -1) {
            if (mBytesRemaining != C.LENGTH_UNSET) {
                throw new EncryptedFileDataSourceException(new EOFException());
            }
            return C.RESULT_END_OF_INPUT;
        }
        // we can't decrement bytes remaining if it's just a flag representation (as opposed to a mutable numeric quantity)
        if (mBytesRemaining != C.LENGTH_UNSET) {
            mBytesRemaining -= bytesRead;
        }
        // notify
       /* if (mTransferListener != null) {
            mTransferListener.onBytesTransferred(this, bytesRead,false,0);
        }*/
        // report
        return bytesRead;
    }

    private int getBytesToRead(int bytesToRead) {
        if (mBytesRemaining == C.LENGTH_UNSET) {
            return bytesToRead;
        }
        return (int) Math.min(mBytesRemaining, bytesToRead);
    }

    @Override
    public Uri getUri() {
        return mUri;
    }


    @Override
    public void close() throws EncryptedFileDataSourceException {
        mUri = null;
        try {
            if (mInputStream != null) {
                mInputStream.close();
            }
        } catch (IOException e) {
            throw new EncryptedFileDataSourceException(e);
        } finally {
            mInputStream = null;
            if (mOpened) {
                mOpened = false;
                /*if (mTransferListener != null) {
                    mTransferListener.onTransferEnd(this,);
                }*/
            }
        }
    }

    public static final class EncryptedFileDataSourceException extends IOException {
        public EncryptedFileDataSourceException(IOException cause) {
            super(cause);
        }
    }

    public static class StreamingCipherInputStream extends CipherInputStream {

        private int mBytesAvailable;

        public StreamingCipherInputStream(InputStream is, Cipher c) {
            super(is, c);
            try {
                mBytesAvailable = is.available();
            } catch (IOException e) {
                // let it be 0
            }
        }

        // if the CipherInputStream has returns 0 from #skip, #read out enough bytes to get where we need to be
        public long forceSkip(long bytesToSkip) throws IOException {
            long processedBytes = 0;
            while (processedBytes < bytesToSkip) {
                long bytesSkipped = skip(bytesToSkip - processedBytes);
                if (bytesSkipped == 0) {
                    if (read() == -1) {
                        throw new EOFException();
                    }
                    bytesSkipped = 1;
                }
                processedBytes += bytesSkipped;
            }
            return processedBytes;
        }

        // We need to return the available bytes from the upstream.
        // In this implementation we're front loading it, but it's possible the value might change during the lifetime
        // of this instance, and reference to the stream should be retained and queried for available bytes instead
        @Override
        public int available() throws IOException {
            return mBytesAvailable;
        }
    }

}