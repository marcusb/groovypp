package shootout.fasta

@Typed
class FastaGroovy {
	static final int IM = 139968
	static final int IA = 3877
	static final int IC = 29573
	static int last = 42

	static int BUFFER_SIZE = 1024
	static int index = 0
	static byte[] bbuffer = new byte[BUFFER_SIZE]
	

	static final int LINE_LENGTH = 60

	// pseudo-random number generator
	static final double random(double max) {
	    last = (last * IA + IC) % IM
	    max * last / IM
	}

	// Weighted selection from alphabet
	static String ALU =
				"GGCCGGGCGCGGTGGCTCACGCCTGTAATCCCAGCACTTTGG" + 
				"GAGGCCGAGGCGGGCGGATCACCTGAGGTCAGGAGTTCGAGA" +
				"CCAGCCTGGCCAACATGGTGAAACCCCGTCTCTACTAAAAAT" +
				"ACAAAAATTAGCCGGGCGTGGTGGCGCGCGCCTGTAATCCCA" +
				"GCTACTCGGGAGGCTGAGGCAGGAGAATCGCTTGAACCCGGG" +
				"AGGCGGAGGTTGCAGTGAGCCGAGATCGCGCCACTGCACTCC" +
				"AGCCTGGGCGACAGAGCGAGACTCCGTCTCAAAAA"

	static byte[] ALUB = ALU.bytes

	static final Frequency[] IUB = [
	        [(char)'a', 0.27d],
	        [(char)'c', 0.12d],
	        [(char)'g', 0.12d],
	        [(char)'t', 0.27d],

	        [(char)'B', 0.02d],
	        [(char)'D', 0.02d],
	        [(char)'H', 0.02d],
	        [(char)'K', 0.02d],
	        [(char)'M', 0.02d],
	        [(char)'N', 0.02d],
	        [(char)'R', 0.02d],
	        [(char)'S', 0.02d],
	        [(char)'V', 0.02d],
	        [(char)'W', 0.02d],
	        [(char)'Y', 0.02d] ]

	static final Frequency[] HomoSapiens = [
	        [(char)'a', 0.3029549426680d],
	        [(char)'c', 0.1979883004921d],
	        [(char)'g', 0.1975473066391d],
	        [(char)'t', 0.3015094502008d]]

	static void makeCumulative(Frequency[] a) {
		double cp = 0
		for (int i = 0; i < a.length; i++) {
			cp += a[i].p;
			a[i].p = cp;
		}
	}

	// naive
	final static byte selectRandom(Frequency[] a) {
	    int len = a.length
	    double r = random(1.0d)
	    for (int i = 0; i < len; i++)
	        if (r < a[i].p)
	            return a[i].c
	    return a[len - 1].c
	}

	static final void makeRandomFasta(String id, String desc, Frequency[] a, int n, OutputStream writer) throws IOException {
	    index = 0
	    int m = 0
	    while (n > 0) {
	        if (n < LINE_LENGTH) m = n;  else m = LINE_LENGTH
	        if(BUFFER_SIZE - index < m) {
	            index = 0
	        }
	        for (int i = 0; i < m; i++) {
	            bbuffer[index++] = selectRandom(a)
	        }
	        bbuffer[index++] = (char)'\n'
	        n -= LINE_LENGTH;
	    }
	}

	static final void makeRepeatFasta(String id, String desc, String alu, int n, OutputStream writer) throws IOException {
	    index = 0
	    int m = 0
	    int k = 0
	    int kn = ALUB.length
	    String descStr = ">" + id + " " + desc + '\n'
//	    writer.write(descStr.getBytes());
	    while (n > 0) {
	        if (n < LINE_LENGTH) m = n; else m = LINE_LENGTH;
	        if(BUFFER_SIZE - index < m) {
//	            writer.write(bbuffer, 0, index);
	            index = 0
	        }
	        for (int i = 0; i < m; i++) {
	            if (k == kn) k = 0
	            bbuffer[index++] = ALUB[k]
	            k++
	        }
	        bbuffer[index++] = (char)'\n'
	        n -= LINE_LENGTH
	    }
//	    if(index != 0) writer.write(bbuffer, 0, index);
	}

	static void main(String[] args) {
		def start = System.currentTimeMillis()
		makeCumulative(HomoSapiens)
		makeCumulative(IUB)
		def n = 25000
		if (args.length > 0)
		    n = Integer.parseInt(args[0])
		OutputStream out = System.out
		makeRepeatFasta("ONE", "Homo sapiens alu", ALU, n * 2, out)
		makeRandomFasta("TWO", "IUB ambiguity codes", IUB, n * 3, out)
		makeRandomFasta("THREE", "Homo sapiens frequency", HomoSapiens, n * 5, out)
		long total = System.currentTimeMillis() - start
		println "[Fasta-Groovy Benchmark Result: $total]"
	}


	static class Frequency {
	    byte c
	    double p

	    public Frequency(char c, double p) {
	        this.c = (byte)c
	        this.p = p
	    }
	}
}
