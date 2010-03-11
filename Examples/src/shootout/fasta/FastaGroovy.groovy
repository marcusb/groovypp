package shootout.fasta

@Typed
class FastaGroovy {
	public static final int IM = 139968;
	public static final int IA = 3877;
	public static final int IC = 29573;
	public static int last = 42;

	static int BUFFER_SIZE = 1024;
	static int index = 0;
	static byte[] bbuffer = new byte[BUFFER_SIZE];
	

	public static final int LINE_LENGTH = 60;

	// pseudo-random number generator
	public static final double random(double max) {
	    last = (last * IA + IC) % IM;
	    return max * last / IM;
	}

	// Weighted selection from alphabet
	public static String ALU =
				"GGCCGGGCGCGGTGGCTCACGCCTGTAATCCCAGCACTTTGG" + 
				"GAGGCCGAGGCGGGCGGATCACCTGAGGTCAGGAGTTCGAGA" +
				"CCAGCCTGGCCAACATGGTGAAACCCCGTCTCTACTAAAAAT" +
				"ACAAAAATTAGCCGGGCGTGGTGGCGCGCGCCTGTAATCCCA" +
				"GCTACTCGGGAGGCTGAGGCAGGAGAATCGCTTGAACCCGGG" +
				"AGGCGGAGGTTGCAGTGAGCCGAGATCGCGCCACTGCACTCC" +
				"AGCCTGGGCGACAGAGCGAGACTCCGTCTCAAAAA";

	public static byte[] ALUB = ALU.getBytes();

	public static final Frequency[] IUB = [
	        new Frequency((char)'a', 0.27),
	        new Frequency((char)'c', 0.12),
	        new Frequency((char)'g', 0.12),
	        new Frequency((char)'t', 0.27),

	        new Frequency((char)'B', 0.02),
	        new Frequency((char)'D', 0.02),
	        new Frequency((char)'H', 0.02),
	        new Frequency((char)'K', 0.02),
	        new Frequency((char)'M', 0.02),
	        new Frequency((char)'N', 0.02),
	        new Frequency((char)'R', 0.02),
	        new Frequency((char)'S', 0.02),
	        new Frequency((char)'V', 0.02),
	        new Frequency((char)'W', 0.02),
	        new Frequency((char)'Y', 0.02) ];

	public static final Frequency[] HomoSapiens = [
	        new Frequency((char)'a', 0.3029549426680d),
	        new Frequency((char)'c', 0.1979883004921d),
	        new Frequency((char)'g', 0.1975473066391d),
	        new Frequency((char)'t', 0.3015094502008d)];

	public static void makeCumulative(Frequency[] a) {
		double cp = 0.0;
		for (int i = 0; i < a.length; i++) {
			cp += a[i].p;
			a[i].p = cp;
		}
	}

	// naive
	public final static byte selectRandom(Frequency[] a) {
	    int len = a.length;
	    double r = random(1.0);
	    for (int i = 0; i < len; i++)
	        if (r < a[i].p)
	            return a[i].c;
	    return a[len - 1].c;
	}

	static final void makeRandomFasta(String id, String desc, Frequency[] a, int n, OutputStream writer) throws IOException
	{
	    index = 0;
	    int m = 0;
	    String descStr = ">" + id + " " + desc + '\n';
	    writer.write(descStr.getBytes());
	    while (n > 0) {
	        if (n < LINE_LENGTH) m = n;  else m = LINE_LENGTH;
	        if(BUFFER_SIZE - index < m){
	            writer.write(bbuffer, 0, index);
	            index = 0;
	        }
	        for (int i = 0; i < m; i++) {
	            bbuffer[index++] = selectRandom(a);
	        }
	        bbuffer[index++] = (char)'\n';
	        n -= LINE_LENGTH;
	    }
	    if(index != 0) writer.write(bbuffer, 0, index);
	}

	static final void makeRepeatFasta(String id, String desc, String alu, int n, OutputStream writer) throws IOException
	{
	    index = 0;
	    int m = 0;
	    int k = 0;
	    int kn = ALUB.length;
	    String descStr = ">" + id + " " + desc + '\n';
	    writer.write(descStr.getBytes());
	    while (n > 0) {
	        if (n < LINE_LENGTH) m = n; else m = LINE_LENGTH;
	        if(BUFFER_SIZE - index < m){
	            writer.write(bbuffer, 0, index);
	            index = 0;
	        }
	        for (int i = 0; i < m; i++) {
	            if (k == kn) k = 0;
	            bbuffer[index++] = ALUB[k];
	            k++;
	        }
	        bbuffer[index++] = (char)'\n';
	        n -= LINE_LENGTH;
	    }
	    if(index != 0) writer.write(bbuffer, 0, index);
	}



	public static void main(String[] args) {
		long start = System.currentTimeMillis();

		makeCumulative(HomoSapiens);
		makeCumulative(IUB);
		int n = 2500000;
		if (args.length > 0)
		    n = Integer.parseInt(args[0]);
		OutputStream out = System.out;
		makeRepeatFasta("ONE", "Homo sapiens alu", ALU, n * 2, out);
		makeRandomFasta("TWO", "IUB ambiguity codes", IUB, n * 3, out);
		makeRandomFasta("THREE", "Homo sapiens frequency", HomoSapiens, n * 5, out);
		long end = (System.currentTimeMillis() - start)/1000;
		println ("Total: " + end);
	}

	public static class Frequency {
	    public byte c;
	    public double p;

	    public Frequency(char c, double p) {
	        this.c = (byte)c;
	        this.p = p;
	    }
	}
}
