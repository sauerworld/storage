(ns sauerworld.storage.core
  (:require [sauerworld.storage.models.users :as users]
            [sauerworld.storage.models.articles :as articles]
            [sauerworld.storage.models.tournaments :as tournaments]
            [sauerworld.storage.db :refer (create-db)]
            [immutant.messaging :as msg]))

(def world (atom {}))

(def db-resource
  "resources/db/main")

(def api-routes
  {:users/insert-user users/insert-user
   :users/get-by-validation-key users/get-by-validation-key
   :users/set-validated users/set-validated
   :users/get-by-username users/get-by-username
   :users/check-login users/check-login
   :users/add-pubkey users/add-pubkey
   :users/update-password users/update-password
   :users/validate-registration users/validate-registration
   :users/validate-password users/validate-password
   :articles/insert-article articles/insert-article
   :articles/update-article articles/update-article
   :articles/find-all-articles articles/find-all-articles
   :articles/find-category-articles articles/find-category-articles
   :tournaments/insert-tournamet tournaments/insert-tournament
   :tournaments/insert-event tournaments/insert-event
   :tournaments/insert-registration tournaments/insert-registration
   :tournaments/get-next-tournament tournaments/get-next-tournament
   :tournaments/get-tournament-by-id tournaments/get-tournament-by-id
   :tournaments/get-current-tournament tournaments/get-current-tournament
   :tournaments/get-tournaments tournaments/get-tournaments
   :tournaments/get-tournament tournaments/get-tournament})

(defn make-api-responder
  [db]
  (fn [{:keys [action params] :as req}]
    (if-let [req-fn (get api-routes action)]
      {:status :ok
       :response (apply req-fn db params)}
      {:status :error})))

(defn start []
  (let [db (create-db db-resource)]
    (do
      (msg/start "queue/storage")
      (msg/respond "queue/storage" (make-api-responder db))
      (swap! world assoc :db db))))

(defn stop []
  (when-let [db (@world :db)]
    (let [datasource (-> db :pool deref :datasource)]
      (.close datasource))))
