import java.util.PriorityQueue;

/**
 *	Interface that all compression suites must implement. That is they must be
 *	able to compress a file and also reverse/decompress that process.
 * 
 *	@author Brian Lavallee
 *	@since 5 November 2015
 *  @author Owen Atrachan
 *  @since December 1, 2016
 *  @author Alice Dai
 *  @since April 15, 2018
 */
public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); // or 256
	public static final int PSEUDO_EOF = ALPH_SIZE; //the fake end number 
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;
	public static final int HUFF_COUNTS = HUFF_NUMBER | 2;

	public enum Header{TREE_HEADER, COUNT_HEADER};
	public Header myHeader = Header.TREE_HEADER;
	
	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){
	   //step 1 find the number of occurrences of each character
		int[] numOccurences = readForCounts(in);
		//step 2 make a Huffman tree using this weighting information
		HuffNode root = makeTreeFromCounts(numOccurences);
		//step 3 make a code of tree traversals throughout the Huffman tree
		String[] codings = makeCodingsFromTree(root);
		//step 4 write the header part that includes the magic number and the tree in preorder traversal
	    writeHeader(root, out);
	    //step 5 reset the text to the beginning an then write the compression, taking 8bits, finding its coding in the huffman tree, then turning that into a bit representation
	    writeCompressedBits(codings, in, out);
	}
	
	/**
	 * helper method for compress that counts the number of occurrences for each character
	 * @param in
	 * 
	 * @return int[] of size 256 that stores all the number of occurrences of each indexed character
	 */
	private int[] readForCounts(BitInputStream in) {
		int[] numOccurrences = new int[256]; //REVISIT
		int val=0;
		while(true) {
		val = in.readBits(BITS_PER_WORD); //reads the next 8 bits and translates that into its numerical equivalent
		System.out.println("Val is " + val);
		if (val!=-1) numOccurrences[val]++; //counter
		else break; //we've ended the file
		}
		return numOccurrences;
	}

	/**
	 * helper method for compress
	 * takes in the array from readForCounts and returns 
	 * a HuffNode that's the root of the Huffman coding tree
	 * 
	 * @param the int[] returned from readForCounts
	 * 
	 * @return HuffNode
	 */
	private HuffNode makeTreeFromCounts(int[] numOccurrences) {
		PriorityQueue<HuffNode> HuffQueue = new PriorityQueue<>(); //sort the HuffNodes into PQs, removes smallest weights
		
		//make nodes and add them to a priority queue
		for (int i=0; i< numOccurrences.length; i++) {
			if (numOccurrences[i]>0) {
			HuffNode current = new HuffNode(i,numOccurrences[i],null,null); //these are the leaves of the HuffTree, value is the 8bit sequence and weight is number of times it occurs
			HuffQueue.add(current);	
			}
		}
		//add PSEUDO_EOF
			HuffQueue.add(new HuffNode(PSEUDO_EOF,1,null,null)); //occurs once 
			
		//now make the tree by using the greedy algorithm, which is perfect since the priority queue is already sorted
		while (HuffQueue.size()>1) {
			HuffNode left = HuffQueue.remove(); //largest
			HuffNode right = HuffQueue.remove(); //second largest
			HuffNode innerNode = new HuffNode(-1, left.weight() + right.weight(), left,right); //this is the parent node
			HuffQueue.add(innerNode);
		}
		HuffNode root = HuffQueue.remove(); //root has the biggest weight 
		return root;
		}
	
	private String[] makeCodingsFromTree(HuffNode root) {
		String[] codings = new String[257];
		return makeCodingsFromTreeHelper(root, "", codings);
	}
	/**
	 * compress helper method that makes a binary string representation for the stuff HuffNode
	 * this is recursive
	 * @param root
	 * @return a StringArray of mappings, where the index is the letter/number and the value is the tree traversal of that letter/number
	 */
	private String[] makeCodingsFromTreeHelper(HuffNode root, String path, String[] codings) {
		if (root==null) return codings; 
		if (root.left()==null && root.right()==null) codings[root.value()]=path; //at leaf node, remember 256 is value of PSEUDO_EOF
		if (root.right()!=null) makeCodingsFromTreeHelper(root.right(), path + "1", codings); //traversing and adding to the code
		if (root.left()!=null) makeCodingsFromTreeHelper(root.left(),path+"0", codings); //traversing and adding to the code
		return codings;
	}
	
	/**
	 * Helper method for compress that writes the header with the magic number and the preorder traversal of the tree 
	 * @param root
	 * @param out
	 */
	private void writeHeader(HuffNode root, BitOutputStream out) {
		//step 1: writing the 32 bit magic HUFF_TREE number 
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		
		//step 2: writing the pre-order traversal, recursively. Good to have a helper method
		writeTree(root,out);
	}
	
	/**
	 * Helper method for writeHeader that recursively writes the huffTree, preOrder traversal 
	 * @param root
	 * @param out
	 */
	private void writeTree(HuffNode root, BitOutputStream out) {
		//base cases
		if (root==null) return;
		if (root.left()==null && root.right()==null) { //at leaf node
			out.writeBits(1,1); //leaf nodes have a value of 1
			out.writeBits(BITS_PER_WORD+1,root.value()); //if it's a leaf, also write a 9 bit sequence that corresponds to the value of the leaf
		}
		//recursion in PREORDER TRAVERSAL
		if(root.left()!=null) {
			out.writeBits(1, 0); //internal nodes have a value of 0
			writeTree(root.left(),out);
		}
		if(root.right()!=null) {
			out.writeBits(1, 0); //internal nodes have a value of 0
			writeTree(root.right(),out);
		}
	}
	
/**
 * resets to the beginning of the file
 * The method reads every BITS_PER_WORD bit-sequence, finds the encoding, and writes the encoding as a bit sequence
 * Basically the point is to take read the file, then convert the words into their new codes via huffman tree and the already made codings from the tree
 * @param mapping
 * @param in
 * @param out
 */
	private void writeCompressedBits(String[] codings, BitInputStream in, BitOutputStream out) {
		in.reset(); //reset the inputStream so we can compress the file from the beginning
		int current;
		String code;
		while (true) {
		current = in.readBits(BITS_PER_WORD);
		if (current==-1) { 
			code=codings[PSEUDO_EOF]; //writing the PSEUDO_EOF end
			out.writeBits(code.length(), Integer.parseInt(code,2)); //the idea is to turn the string into an integer with parseInt and then turning it into bit representation 
			break;
		}
		else {
			code = codings[current]; //finds the string representation of this letter through the tree
			out.writeBits(code.length(), Integer.parseInt(code,2)); 
		}
		}
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
	    //step 1: read the 32 bit magic number to check if the file is able to to be decompressed by our decompressor
		int id= in.readBits(BITS_PER_INT);
		if (id!=(HUFF_TREE)) throw new HuffException("not the magic number"); //not equal means file is not a compressed file	
		//step 2: make the tree
		HuffNode root = readTreeHeader(in);
		//step 3: traverse the tree and read the decompress the file
	    readCompressedBits(root,in,out);
	}
	
	/**helper method for decompress, magic number already called and reads
	 * the first bit of the pre-order traversal of the Huffman tree
	 * 
	 * @param in
	 * @return a HuffNode that's the root of the Huffman tree
	 */
	private HuffNode readTreeHeader(BitInputStream in) {
		//read one bit and check the value
		int bitVal = in.readBits(1);
		if (bitVal == -1) throw new NullPointerException("Nothing read"); // signal error, nothing read
		if (bitVal==0) { //make two recursive calls 
			HuffNode left= readTreeHeader(in); //preorder traversal
			HuffNode right= readTreeHeader(in); //preorder traversal
			return new HuffNode(0,0,left, right); //this is making the tree with children as left and right
		}
		//base case stops the entire method 
		if (bitVal==1) {
			int character = in.readBits(BITS_PER_WORD+1); //9 bits represent the character stored in each leaf
			return new HuffNode(character,0); //this is a base case, the weight doesn't matter so I set it to 0, and its a leaf so left and right are null (default)
		}
		return new HuffNode(0,0); //it shouldn't ever need to get down here
	}
	
	/**
	 *helper method for decompress
	 *once you've created your tree, it's time to decode the compressed algorithm 
	 * @param header
	 */
	private void readCompressedBits(HuffNode root, BitInputStream in, BitOutputStream out) { //the third part of the compressed string of numbers
		HuffNode pointer = root;
		int currentBit;
		while(pointer.value()!=PSEUDO_EOF) {
			//ok so first read a bit
			currentBit = in.readBits(1);
			if (currentBit == -1)  throw new HuffException("bad input, no PSEUDO_EOF");
			//then traverse down the tree
			if (currentBit==0) pointer=pointer.left(); //if you get a zero, go left
			else pointer=pointer.right(); //if you get a one, go right
			//once you get to a leaf
			if (pointer.left()==null && pointer.right()==null) {
				if (pointer.value()==PSEUDO_EOF) break; //if we get to the end of the file
				else {
				out.writeBits(BITS_PER_WORD,pointer.value()); //writes the value in 8 bits
				pointer = root; //start back at the beginning
				}
			}
		}
	}
	
	
	public void setHeader(Header header) {
        myHeader = header;
        System.out.println("header set to "+myHeader);
    }
}