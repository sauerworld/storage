(ns sauerworld.storage.models.tournaments
  (:require [korma.core :as k]
            [sauerworld.storage.utils :refer :all]
            [sauerworld.storage.models.users :as users]
            [clj-time.core :refer (date-time now minus days)]
            [clj-time.coerce :refer (to-date to-date-time)]))

(defn base-tournaments-query
  [db]
  (-> (k/create-entity "tournaments")
      (k/database db)))

(defn base-events-query
  [db]
  (-> (k/create-entity "events")
      (k/database db)))

(defn base-registrations-query
  [db]
  (-> (k/create-entity "registrations")
      (k/database db)))

(defn insert-tournament
  [db {:keys [name date registration-open] :as tournament}]
  (let [registration-open (or registration-open false)
        date (to-date date)
        t-entity {:name name
                  :date date
                  :registration_open registration-open}]
    (-> (base-tournaments-query db)
        (k/insert
         (k/values t-entity))
        (vals))))

(defn insert-event
  [db {:keys [tournament name team-mode]}]
  (let [team-mode (or team-mode false)
        tournament (if (number? tournament)
                     (int tournament)
                     (-> tournament :id int))]
    (-> (base-events-query db)
        (k/insert
         (k/values {:name name
                    :tournament tournament
                    :team_mode team-mode}))
        (vals))))

(defn insert-registration
  [db {:keys [event user team created]}]
  (let [user (if (number? user)
               (int user)
               (-> user :id int))
        event (if (number? event)
                (int event)
                (-> event :id int))
        team (or team "")
        created (if created
                  (to-date created)
                  (to-date (now)))]
    (-> (base-registrations-query db)
        (k/insert
         (k/values {:event event
                    :user user
                    :team team
                    :created created}))
        (vals))))

(defn get-next-tournament
  [db & [date]]
  (let [date (to-date (or date (now)))]
    (-> (base-tournaments-query db)
        (k/select
         (k/where {:date [> date]})
         (k/order :date :asc))
        first)))

(defn get-tournament-by-id
  [db id]
  (-> (base-tournaments-query db)
      (k/select
       (k/where {:id id})
       (k/limit 1))
      first))

(defn get-current-tournament
  [db & [date]]
  (let [date (to-date-time (or date (now)))
        yesterday (-> date
                      (minus (days 1))
                      to-date)]
    (-> (base-tournaments-query db)
        (k/select
         (k/where {:date [> yesterday]})
         (k/order :date :asc))
        first)))

(defn get-tournament-events
  [db tournament]
  {:pre [(number? (:id tournament))]}
  (let [id (:id tournament)]
    (-> (base-events-query db)
        (k/select
         (k/where {:tournament id})))))

(defn get-event-signups
  "Gets signups for a given event (optionally for a given user)."
  [db event & [user]]
  (let [id (cond
            (map? event) (-> event :id vector)
            (and (coll? event) (map? (first event))) (map :id event)
            (coll? event) event
            :else (vector event))
        base-query (-> (base-registrations-query db)
                       (k/select*)
                       (k/where {:event [in id]}))
        user (when user
               (cond (map? user) (:id user)
                     (number? user) user
                     :else nil))
        final-query (if user
                      (k/where base-query {:user user})
                      base-query)]
    (k/exec final-query)))

(defn get-tournament-signups
  [db tournament]
  (let [id (if (number? tournament)
             (int tournament)
             (-> tournament :id int))
        events (get-tournament-events db id)]
    (-> (base-registrations-query db)
        (k/select
         (k/join :inner
                 (k/create-entity "events")
                 (= :events.id :event))
         (k/join (k/create-entity "users")
                 (= :users.id :user))
         (k/where {:events.tournament id})))))

(defn get-tournament
  "Gets a tournament, possibly joined with its events, registrations and users.
   Pass :events as an arg to join events, pass :registrations to join
   registrations, and pass :users to join users."
  [db tournament & args]
  (let [tournament (if (number? tournament)
                     (get-tournament-by-id db tournament)
                     tournament)
        argmap (->> (interleave args (repeat true))
                    (apply hash-map))]
    (if-not (argmap :events)
      tournament
      (let [events (get-tournament-events db tournament)]
        (if-not (argmap :registrations)
          (assoc tournament :events events)
          (let [registrations (get-event-signups db events)]
            (if-not (argmap :users)
              (->> (join-has-many events registrations
                                  :id :event :registrations)
                   (assoc tournament :events))
              (let [user-ids (map :user registrations)
                    users (users/get-by-id db user-ids)
                    joined-registrations (join-belongs-to
                                          registrations users
                                          :user :id :user)
                    joined-events (join-has-many
                                   events joined-registrations
                                   :id :event :registrations)]
                (assoc tournament :events joined-events)))))))))

(defn get-tournaments
  "Gets all tournament, possibly joined with events and registrations as in
   get-tournament."
  [db & args]
  (let [tournaments (-> (base-tournaments-query db)
                        (k/select))]
    (map
     (fn [t] (apply get-tournament db t args))
     tournaments)))

(defn get-event-by-id
  [db event]
  (if (map? event)
    event
    (-> (base-events-query db)
        (k/select
         (k/where {:id event})
         (k/limit 1))
        first)))

(defn get-tournament-for-event
  [db event]
  (when-let [event (cond
                  (number? event) (get-event-by-id db event)
                  (and (map? event) (:tournament event)) event
                  :else nil)]
    (get-tournament-by-id db (:tournament event))))

(defn update-team
  [db registration team]
  (when-let [id (:id registration)]
    (-> (base-registrations-query db)
        (k/update
         (k/set-fields {"team" team})
         (k/where {:id id})))))

(defn delete-registration
  [db registration]
  (let [registration (cond
                      (map? registration) (:id registration)
                      (number? registration) registration
                      :else nil)]
    (when registration
      (-> (base-registrations-query db)
          (k/delete
           (k/where {:id registration}))))))
