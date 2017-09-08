(ns clojure-getting-started.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]
            [camel-snake-kebab.core :as kebab]
            [taoensso.carmine :as car :refer (wcar)]))

(def sample (env :sample "sample-string-thing"))

(def server1-conn {:pool {} :spec {:uri (env :redis-url)}}) ; See `wcar` docstring for opts
(defmacro wcar* [& body] `(car/wcar server1-conn ~@body))

(defn record [input]
  (wcar* 
    (car/lpush :sayings input)))

(defn samples []
  (wcar* 
    (car/lrange :sayings 0 -1)))

(defn splash []
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (concat (for [kind ["camel" "snake" "kebab"]]
   (format "<a href=\"/%s?input=%s\">%s %s</a><br />"
           kind sample kind sample))
 ["<hr /><ul>"]
 (for [s (samples)]
   (format "<li>%s</li>" s))
 ["</ul>"])})

(defroutes app
  (GET "/camel" {{input :input} :params}
    (record input)
       {:status 200
        :headers {"Content-Type" "text/plain"}
        :body (kebab/->CamelCase input)})
  (GET "/snake" {{input :input} :params}
    (record input)
       {:status 200
        :headers {"Content-Type" "text/plain"}
        :body (kebab/->snake_case input)})
  (GET "/kebab" {{input :input} :params}
    (record input)
       {:status 200
        :headers {"Content-Type" "text/plain"}
        :body (kebab/->kebab-case input)})
  (GET "/" []
       (splash))
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app) {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
