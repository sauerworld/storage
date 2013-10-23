(ns sauerworld.storage.utils)

(defn assoc-if
  [m cond k v]
  (let [assoc-fn (if (vector? k) assoc-in assoc)]
    (if cond
      (assoc-fn m k v)
      m)))

(defn vec-wrap-if
  [cond m]
  (if cond
    [m]
    m))

(defn join-has-many
  "Takes a collection of (or single) parent entities, and a collection of child
   entities, and joins them where the foreign key child-k matches the parent id
   key parent-k. The children will be joined into the parent entity under the
   child-name key."
  [parent-ms child-ms parent-k child-k child-name]
  (let [parent-ms (vec-wrap-if (map? parent-ms) parent-ms)
        child-ms (vec-wrap-if (map? child-ms) child-ms)
        grouped-children (group-by #(get % child-k) child-ms)
        join-fn (fn [parent]
                  (let [parent-v (get parent parent-k)]
                    (if-let [children (get grouped-children parent-v)]
                      (assoc parent child-name children)
                      parent)))]
    (let [joined (map join-fn parent-ms)]
      (if (= 1 (count joined))
        (first joined)
        joined))))

(defn join-belongs-to
  "Takes a collection of (or single) belongs-to entities, and a collection of
   the has-one entities, and returns the belongs-to entities with the has-ones
   joined under the belongs-name key."
  [belongs-ms has-ms belongs-k has-k belongs-name]
  (let [belongs-ms (vec-wrap-if (map? belongs-ms) belongs-ms)
        has-ms (vec-wrap-if (map? has-ms) has-ms)
        has-by-k (reduce
                  (fn [col h] (let [k (get h has-k)]
                                (assoc col k h)))
                  {}
                  has-ms)
        join-fn (fn [belongs]
                  (let [belongs-v (get belongs belongs-k)]
                    (if-let [has-v (get has-by-k belongs-v)]
                      (assoc belongs belongs-name has-v)
                      belongs)))]
    (let [joined (map join-fn belongs-ms)]
      (if (= 1 (count joined))
        (first joined)
        joined))))
