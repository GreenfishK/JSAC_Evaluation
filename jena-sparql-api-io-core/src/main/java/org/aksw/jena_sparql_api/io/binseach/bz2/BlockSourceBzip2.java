package org.aksw.jena_sparql_api.io.binseach.bz2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aksw.jena_sparql_api.io.binseach.Block;
import org.aksw.jena_sparql_api.io.binseach.BlockSource;
import org.aksw.jena_sparql_api.io.binseach.BufferFromInputStream;
import org.aksw.jena_sparql_api.io.binseach.BufferFromInputStream.ByteArrayChannel;
import org.aksw.jena_sparql_api.io.binseach.CharSequenceFromSeekable;
import org.aksw.jena_sparql_api.io.binseach.DecodedDataBlock;
import org.aksw.jena_sparql_api.io.binseach.ReverseCharSequenceFromSeekable;
import org.aksw.jena_sparql_api.io.binseach.Seekable;
import org.aksw.jena_sparql_api.io.binseach.SeekableSource;
import org.aksw.jena_sparql_api.io.common.Reference;
import org.aksw.jena_sparql_api.io.common.ReferenceImpl;
import org.aksw.jena_sparql_api.io.deprecated.MatcherFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import com.github.jsonldjava.shaded.com.google.common.primitives.Ints;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;


public class BlockSourceBzip2
    implements BlockSource
{
//	public static final byte[] magic = new BigInteger("425a6839", 16).toByteArray();
    // The magic number in characters is: BZh91AY&SY
    // TODO Block size is a parameter - we should parse it out
    public static final String magicStr = "BZh91AY&SY";
//    public static final byte[] magic = new BigInteger("425a6839314159265359", 16).toByteArray();
    public static final Pattern fwdMagicPattern = Pattern.compile(magicStr, Pattern.LITERAL);
    public static final Pattern bwdMagicPattern = Pattern.compile(new StringBuilder(magicStr).reverse().toString(), Pattern.LITERAL);

    protected SeekableSource seekableSource;
//    protected MatcherFactory fwdBlockStartMatcherFactory;
//    protected MatcherFactory bwdBlockStartMatcherFactory;

    protected Cache<Long, Reference<Block>> blockCache = CacheBuilder.newBuilder().build();



    public BlockSourceBzip2(
            SeekableSource seekableSource,
            MatcherFactory fwdBlockStartMatcherFactory,
            MatcherFactory bwdBlockStartMatcherFactory) {
        super();
        this.seekableSource = seekableSource;
//        this.fwdBlockStartMatcherFactory = fwdBlockStartMatcherFactory;
//        this.bwdBlockStartMatcherFactory = bwdBlockStartMatcherFactory;
    }


    public static BlockSource create(SeekableSource seekableSource) {
//        String str = new String(magic);
//        System.out.println("Str: " + str);
        if(!seekableSource.supportsAbsolutePosition()) {
            throw new RuntimeException("The seekable source must support absolution positions");
        }

        // TODO Turn into constant

//        MatcherFactory fwdMatcher = BoyerMooreMatcherFactory.createFwd(magic);
//        MatcherFactory bwdMatcher = BoyerMooreMatcherFactory.createBwd(magic);


        return new BlockSourceBzip2(seekableSource, null, null);
    }


    protected Reference<Block> loadBlock(Seekable seekable) throws IOException {
        long blockStart = seekable.getPos();

        // The input stream now owns the seekable - closing it closes the seekable!
        InputStream rawIn = Channels.newInputStream(seekable);
        BZip2CompressorInputStream decodedIn = new BZip2CompressorInputStream(rawIn, false);
        BufferFromInputStream blockBuffer = new BufferFromInputStream(8192, decodedIn);

        // Closing the block would close the input stream -
        // In order to allow multiple clients, wrap the block in a reference:
        // Only if there are no more client for a block then the block itself gets closed
        Block block = new DecodedDataBlock(this, blockStart, blockBuffer);
        Reference<Block> result = ReferenceImpl.create(block, block::close, "Root ref to block " + blockStart);

        return result;
    }


    @Override
    public Reference<Block> contentAtOrBefore(long requestPos, boolean inclusive) throws IOException {
        // If the requestPos is already in the cache, serve it from there
        // TODO Track consecutive blocks in a cache
//        if(!inclusive) {
//            inclusive = true;
//        }

        long internalRequestPos = requestPos - (inclusive ? 0 : 1) + (magicStr.length() - 1);
        Reference<Block> result = blockCache.getIfPresent(internalRequestPos);

        if(result == null) {
            Seekable seekable = seekableSource.get(internalRequestPos);
//            System.out.println("Size: " + seekableSource.size());


//            SeekableMatcher matcher = bwdBlockStartMatcherFactory.newMatcher();
//            boolean didFind = matcher.find(seekable);
            CharSequence charSequence = new ReverseCharSequenceFromSeekable(seekable, 0, Ints.saturatedCast(internalRequestPos + 1));
            Matcher matcher = bwdMagicPattern.matcher(charSequence);
            boolean didFind = matcher.find();

            if(didFind) {
                // Move to the beginning of the pattern
                // seekable.prevPos(magic.length - 1);

                // long blockStart = seekable.getPos();
                int end = matcher.end();
                long blockStart = seekable.getPos() - (end - 1);
                seekable.setPos(blockStart);

                result = cache(blockStart, seekable);
            }
        }

        return result == null ? null : result.acquire(null);
    }

    public Reference<Block> cache(long blockStart, Seekable seekable) throws IOException {
        Reference<Block> result;
        try {
            result = blockCache.get(blockStart, () -> loadBlock(seekable));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public Reference<Block> contentAtOrAfter(long requestPos, boolean inclusive) throws IOException {
        // TODO Track consecutive blocks in a cache
//        if(!inclusive) {
//            inclusive = true;
//        }
        long internalRequestPos = requestPos + (inclusive ? 0 : 1);
        Reference<Block> result = blockCache.getIfPresent(internalRequestPos);

        if(result == null) {
            Seekable seekable = seekableSource.get(internalRequestPos);
//            SeekableMatcher matcher = fwdBlockStartMatcherFactory.newMatcher();
//            boolean didFind = matcher.find(seekable);

            CharSequence charSequence = new CharSequenceFromSeekable(seekable, 0, Ints.saturatedCast(seekableSource.size() - internalRequestPos));
            Matcher matcher = fwdMagicPattern.matcher(charSequence);
            boolean didFind = matcher.find();

            if(didFind) {
                long blockStart = seekable.getPos() + matcher.start();
                seekable.setPos(blockStart);
                // We are now at the beginning of the pattern
                //long blockStart = seekable.getPos();
//                System.err.println("Bz2 block for " + requestPos + " found at " + blockStart);
                result = cache(blockStart, seekable);
            }
        }

        return result == null ? null : result.acquire(null);
    }

    @Override
    public long getSizeOfBlock(long pos) throws IOException {
        // TODO If the pos is not an exact offset, raise an error
        // TODO The block size may be known - e.g. 900K - in that case we only need to check whether there
        // is subsequent block - only if there is none we actually have to compute the length
        long result;
        try (Reference<Block> ref = contentAtOrAfter(pos, true)) {
            try (Seekable channel = ref.get().newChannel()) {
            	// This is super ugly code to read all data in a block
            	// in order to get its size
            	result = ((ByteArrayChannel)channel).loadAll();
            }
        } catch (Exception e) {
            throw new IOException(e);
        }

        return result;
    }



    @Override
    public boolean hasBlockAfter(long pos) throws IOException {
        boolean result;
        try(Seekable seekable = seekableSource.get(pos + 1)) {
//            SeekableMatcher matcher = fwdBlockStartMatcherFactory.newMatcher();
//            result = matcher.find(seekable);
            CharSequence charSequence = new CharSequenceFromSeekable(seekable, 0, Ints.saturatedCast(seekableSource.size() - (pos + 1)));
            Matcher matcher = fwdMagicPattern.matcher(charSequence);
            result = matcher.find();
        }
        return result;
    }

    @Override
    public boolean hasBlockBefore(long pos) throws IOException {
        boolean result;
        long internalRequestPos = pos - 1 + (magicStr.length() - 1);
        try(Seekable seekable = seekableSource.get(internalRequestPos)) {
//            SeekableMatcher matcher = bwdBlockStartMatcherFactory.newMatcher();
//            result = matcher.find(seekable);
//            try(Seekable clone = seekable.clone()) {
            CharSequence charSequence = new ReverseCharSequenceFromSeekable(seekable, 0, Ints.saturatedCast(internalRequestPos + 1));
            Matcher matcher = bwdMagicPattern.matcher(charSequence);
            result = matcher.find();
//            }
        }
        return result;
    }


    @Override
    public long size() throws IOException {
        long result = seekableSource.size();
        return result;
    }


//	@Override
//	public long getXBoundBefore(long pos) {
//		Seekable seekable = seekableSource.get(pos);
//
//
//
//		// Get a seekable for the given position
//		// Scan the seekable in reverse for the magic bytes
//
//		return 0;
//	}
    // Hack to set up an empty bz2 stream in order to reuse the header
    // this way we don't have to copy and adapt the BZip2CompressorInputStream class
//	ByteArrayOutputStream headerOut = new ByteArrayOutputStream();
//	OutputStream out = new BZip2CompressorOutputStream(headerOut);
//	out.flush();
//	out.close();
//	byte[] headerBytes = headerOut.toByteArray();
//	int headerLen = 0; //headerBytes.length; //12; //Bytes.indexOf(headerBytes, magic);

//	System.out.println(headerBytes.length);

//
//	byte[] test = new byte[16];
//	seekable.peekNextBytes(test, 0, 16);
//
//	System.out.println(new String(test));
//	System.out.println("blockStart: " + blockStart);




}
