(ns sauerworld.storage.core
  (:require [sauerworld.storage.models.users :as users]
            [sauerworld.storage.models.articles :as articles]
            [sauerworld.storage.models.tournaments :as tournaments]
            [sauerworld.storage.db :refer (create-db)]
            [immutant.messaging :as msg]
            [immutant.util :refer (in-immutant? app-relative at-exit)]))

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
   :articles/find-article articles/find-article
   :articles/find-all-articles articles/find-all-articles
   :articles/find-category-articles articles/find-category-articles
   :tournaments/insert-tournamet tournaments/insert-tournament
   :tournaments/insert-event tournaments/insert-event
   :tournaments/insert-registration tournaments/insert-registration
   :tournaments/get-next-tournament tournaments/get-next-tournament
   :tournaments/get-tournament-by-id tournaments/get-tournament-by-id
   :tournaments/get-current-tournament tournaments/get-current-tournament
   :tournaments/get-tournaments tournaments/get-tournaments
   :tournaments/get-tournament tournaments/get-tournament
   :tournaments/get-event-by-id tournaments/get-event-by-id
   :tournaments/get-tournament-for-event tournaments/get-tournament-for-event
   :tournaments/get-event-signups tournaments/get-event-signups
   :tournaments/update-team tournaments/update-team
   :tournaments/delete-registration tournaments/delete-registration})

(defn make-api-responder
  [db]
  (fn [{:keys [action params] :as req}]
    (if-let [req-fn (get api-routes action)]
      {:status :ok
       :response (apply req-fn db params)}
      {:status :error})))

(defn stop []
  (when-let [db (@world :db)]
    (let [datasource (-> db :pool deref :datasource)]
      (.close datasource)))
  (when-let [listener (@world :listener)]
    (msg/unlisten listener)))

(defn start []
  (msg/start "queue/storage")
  (let [db-path (if (in-immutant?)
                  (-> db-resource
                      app-relative
                      str)
                  db-resource)
        db (create-db db-path)
        listener (msg/respond "queue/storage" (make-api-responder db))]
    (do
      (swap! world assoc :db db)
      (swap! world assoc :listener listener)
      (at-exit stop))))
