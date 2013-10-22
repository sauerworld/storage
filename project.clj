(defproject sauerworld/storage "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.3.0-alpha5"]
                 [korma "0.3.0-RC6"]
                 [com.h2database/h2 "1.3.174"]
                 [clojurewerkz/scrypt "1.0.0"]
                 [com.novemberain/validateur "1.5.0"]]
  :profiles {:dev
             {:source-paths ["dev"]
              :dependencies [[org.clojars.jcrossley3/tools.namespace "0.2.4.1"]
                             [org.immutant/immutant "1.0.1"]
                             [ring-mock "0.1.5"]]
              :immutant {:nrepl-port 0}}}
  :immutant {:init "sauerworld.storage.core/start"})
