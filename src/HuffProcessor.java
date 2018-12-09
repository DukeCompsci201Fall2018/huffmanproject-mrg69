import java.util.PriorityQueue;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){
		
		int[] counts = readForCounts(in);
		HuffNode root = makeTreeFromCounts(counts);
		String[] codings = makeCodingsFromTree(root, "");
		
		out.writeBits(BITS_PER_INT,  HUFF_TREE);;
		writeHeader(root, out);
		
		in.reset();
		writeCompressedBits(codings, in, out);
		out.close();
	}
	
	
	private void writeCompressedBits(String[] codings, BitInputStream in, BitOutputStream out) {
		
		while (true) {
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1) break;
			String a = codings[val];
			out.writeBits(a.length(), Integer.parseInt(a, 2));
			
		}
		String b = codings[PSEUDO_EOF];
	    out.writeBits(b.length(), Integer.parseInt(b,2));

		out.writeBits(codings[256].length(), Integer.parseInt(codings[256], 2));
	}

	private void writeHeader(HuffNode root, BitOutputStream out) {
		
		if (root.myLeft != null || root.myRight != null) {
			out.writeBits(1, 0);
			writeHeader(root.myLeft, out);
			writeHeader(root.myRight, out);
			
		} 
		
		else {
			out.writeBits(1,  1);
			out.writeBits(9,  root.myValue);
			
		}
		
	}


	//public String[] list = new String[257];
	//public String[] encodings = new String[ALPH_SIZE + 1];
	private String[] makeCodingsFromTree(HuffNode root, String a) {
		String[] encodings = new String[ALPH_SIZE + 1];
	    codingHelper(root,"",encodings);


		/*if (root.myLeft == null && root.myRight == null) {
			encodings[root.myValue] = a;
		}
		else {
			makeCodingsFromTree(root.myLeft, a + "0");
			makeCodingsFromTree(root.myRight, a + "1");
		} */
		
		
		return encodings;
	}
	
	
	
	private void codingHelper(HuffNode root, String a, String[] encodings) {
		if (root.myLeft == null && root.myRight == null) {
	        encodings[root.myValue] = a;
	        return;
	        
	   }
		else {
			codingHelper(root.myLeft, a + "0", encodings);
			codingHelper(root.myRight, a + "1", encodings);
		}
	} 
	
	
	
	
	
	private HuffNode makeTreeFromCounts(int[] counts) {

		PriorityQueue<HuffNode> a = new PriorityQueue<HuffNode>();
		for (int i = 0; i < counts.length; i++) {
			if (counts[i] > 0) a.add(new HuffNode(i, counts[i], null, null));
		}

		a.add(new HuffNode(PSEUDO_EOF, 0, null, null));
		
		while (a.size() > 1) {
			HuffNode myLeft = a.remove();
			HuffNode myRight = a.remove();
			a.add(new HuffNode(0, myLeft.myWeight + myRight.myWeight, myLeft, myRight));
			
		}
		HuffNode root = a.remove();
		return root;
		
	}

	private int[] readForCounts(BitInputStream in) {
		
		int[] size = new int[ALPH_SIZE + 1];
		int val = in.readBits(BITS_PER_WORD);
		while (val != -1) {
			size[val]++;
			val = in.readBits(BITS_PER_WORD);
			
		}
		size[PSEUDO_EOF] = 1;
		return size;
	}

	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out){

		int val = in.readBits(BITS_PER_INT);
			
		if (val != HUFF_TREE) {
			throw new HuffException("illegal header starts with " + val);
		}
		
		HuffNode root = readTreeHeader(in);
		
		readCompressedBits(root, in, out);
		out.close();
	}

	private HuffNode readTreeHeader(BitInputStream in) {
		
		int val = in.readBits(1);
		
		if (val == 0) return new HuffNode(0, 0, readTreeHeader(in), readTreeHeader(in));
		
		return new HuffNode(in.readBits(9), 0, null, null);
	}

	
	private void readCompressedBits(HuffNode root, BitInputStream in, BitOutputStream out) {
		
		
		HuffNode a = root;
		while (true) {
			
			int b = in.readBits(1);
			if (b == -1) throw new HuffException("No PSEUDO_EOF!");
			
			if (b == 0) a = a.myLeft;
			if (b == 1) a = a.myRight;
			
			if (a.myLeft == null && a.myRight == null) {
				if (a.myValue == PSEUDO_EOF) break;
				
				out.writeBits(BITS_PER_WORD, a.myValue);
				a = root;
			}
			
		}
	}
	
	
	
}