package shootout

class BenchmarkResultPrinter {
	public static void main(String[] args) {
		println "====== BENCHMARK RESULTS ======"
		new File('benchmarks.txt').eachLine {
			ln -> if (ln =~ '\\[.* Benchmark Result:*') {
				println ln
			}
		}
	}
}