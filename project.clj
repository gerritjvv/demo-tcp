(defproject demo-tcp "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]

  :dependencies [
                 [org.clojure/tools.logging "0.2.3"]
                 [io.netty/netty-all "4.0.24.Final"]
                 [org.clojure/clojure "1.6.0"]])
