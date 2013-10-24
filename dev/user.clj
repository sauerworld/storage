(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.set :as set]
            [clojure.pprint :refer (pprint)]
            [korma.core :as k]
            [korma.db :as kdb]
            [sauerworld.storage.db :as db]
            [sauerworld.storage.core :as core]
            [sauerworld.storage.models.users :as users]
            [sauerworld.storage.models.articles :as articles]
            [sauerworld.storage.models.tournaments :as tournaments]
            [immutant.messaging :as msg]
            [immutant.util :refer :all]
            [clojure.tools.namespace.repl :refer (refresh)]))

(def db-path
  (let [base-path "resources/db/main"]
    (if (in-immutant?)
      (str (app-relative base-path))
      base-path)))

(def db-spec
  (db/create-h2-spec db-path))

(require '[clojure.java.jdbc :as j])

(defn do-db-test
  [path]
  (j/query (db/create-h2-spec path) ["SHOW TABLES"]))
