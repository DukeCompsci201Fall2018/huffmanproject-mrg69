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
		//original skeleton code
//		while (true){
//			int val = in.readBits(BITS_PER_WORD);
//			if (val == -1) break;
//			out.writeBits(BITS_PER_WORD, val);
//		}
//		out.close();
		
		int[] counts = readForCounts(in);
		HuffNode root = makeTreeFromCounts(counts);
		
	}
	private HuffNode makeTreeFromCounts(int[] nums) {
		// TODO Auto-generated method stub
		PriorityQueue<HuffNode> pq = new PriorityQueue<HuffNode>();
		for (int i = 0; i < nums.length; i++) {
			if (nums[i] > 0) pq.add(new HuffNode(i, nums[i], null, null));
		}
		
		pq.add(new HuffNode(PSEUDO_EOF, 0, null, null));
		
		while(pq.size() > 1) {
			HuffNode myLeft = pq.remove();
			HuffNode myRight = pq.remove();
			pq.add(new HuffNode(0, myLeft.myWeight + myRight.myWeight, myLeft, myRight));
		}
		return pq.remove();
	}

	private int[] readForCounts(BitInputStream in) {
		// TODO Auto-generated method stub
		int [] size = new int[ALPH_SIZE + 1];
		int val = in.readBits(BITS_PER_WORD);
		while(val != -1) {
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

		//keeping starter code for reference
//		while (true){
//			int val = in.readBits(BITS_PER_WORD);
//			if (val == -1) break;
//			out.writeBits(BITS_PER_WORD, val);
//	}
		
		int val = in.readBits(BITS_PER_INT);
		if (val != HUFF_TREE) {
			throw new HuffException("illegal header starts withh" +val);
		}
		
		HuffNode root = readTreeHeader(in);
		readCompressedBits(root, in, out);
		out.writeBits(BITS_PER_WORD, val);
		out.close();
	}

	private void readCompressedBits(HuffNode root, BitInputStream in, BitOutputStream out) {
		// TODO Auto-generated method stub
		HuffNode h = root;
		while (true) {
			int i = in.readBits(1);
			if (i == -1) throw new HuffException("No PSEUDO_EOF");
			//Traversing HuffNodes; 0 for left, 1 for right
			if (i == 0) h = h.myLeft;
			if (i == 1) h = h.myRight;
			if (h.myLeft == null && h.myRight == null) {
				if (h.myValue == PSEUDO_EOF) break;
				out.writeBits(BITS_PER_WORD, h.myValue);
				h = root;
			}
		}
	}

	private HuffNode readTreeHeader(BitInputStream in) {
		// TODO Auto-generated method stub
		int val = in.readBits(1);
		
		if (val == 0) return new HuffNode(0,0,readTreeHeader(in), readTreeHeader(in));
		//9 is BITS_PER_WORD + 1
		return new HuffNode(in.readBits(9),0, null, null);
	}
}