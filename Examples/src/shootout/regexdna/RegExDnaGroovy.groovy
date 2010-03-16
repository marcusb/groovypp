package shootout.regexdna

import java.util.regex.Matcher
import java.util.regex.Pattern

class RegExDnaGroovy {

	private static final Map<String, String> replacements = new HashMap<String, String>();

	static {

		replacements.put("W", "(a|t)");
		replacements.put("Y", "(c|t)");
		replacements.put("K", "(g|t)");
		replacements.put("M", "(a|c)");
		replacements.put("S", "(c|g)");
		replacements.put("R", "(a|g)");
		replacements.put("B", "(c|g|t)");
		replacements.put("D", "(a|g|t)");
		replacements.put("V", "(a|c|g)");
		replacements.put("H", "(a|c|t)");
		replacements.put("N", "(a|c|g|t)");
	}

	static void main(String[] args) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(System.in,
				"US-ASCII"));
		StringBuffer sb = new StringBuffer()
		String line
		while ((line = r.readLine()) != null) {
			sb.append(line)
			sb.append("\n")
		}

		int initialLength = sb.length()

		final String sequence = sb.toString().replaceAll(">.*\n|\n", "")

		int codeLength = sequence.length()

		String[] variants = ["agggtaaa|tttaccct", "[cgt]gggtaaa|tttaccc[acg]",
				"a[act]ggtaaa|tttacc[agt]t", "ag[act]gtaaa|tttac[agt]ct",
				"agg[act]taaa|ttta[agt]cct", "aggg[acg]aaa|ttt[cgt]ccct",
				"agggt[cgt]aa|tt[acg]accct", "agggta[cgt]a|t[acg]taccct",
				"agggtaa[cgt]|[acg]ttaccct"]

		final Map<String, Integer> results = new HashMap<String, Integer>()
		ThreadGroup tg = new ThreadGroup("regexWork")
		for (String v: variants) {
			final String variant = v
			new Thread(tg, v) {
				void run() {
					int count = 0
					Matcher m = Pattern.compile(variant).matcher(sequence)
					while (m.find()) {
						count++
					}
					results.put(variant, count)
				}
			}.start()
		}
		Thread[] threads = new Thread[variants.length]
		tg.enumerate(threads)
		for (Thread t: threads) {
			try {
				if (t != null) {
					t.join()
				}
			} catch (InterruptedException e) {
				// noop
			}
		}
		tg.destroy()
		for (String variant: variants) {
			println variant + " " + results.get(variant)
		}
		StringBuffer buf = new StringBuffer()
		Matcher m = Pattern.compile("[WYKMSRBDVHN]").matcher(sequence)
		while (m.find()) {
			m.appendReplacement(buf, "")
			buf.append(replacements.get(m.group()))
		}
		m.appendTail(buf)

		println ""
		println initialLength
		println codeLength
		buf.length()
	}
}
