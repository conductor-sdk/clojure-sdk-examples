(ns io-orkes.clojure-sdk-examples
  (:require [io.orkes.workflow-resource :as wr]
            [io.orkes.sdk :as sdk]
            [io.orkes.metadata :as metadata]
            [io.orkes.taskrunner :as tr])
  (:gen-class))

(def constants
  {:get-user-info "get_user_info"
   :send-email "send_email"
   :send-sms "send_sms"
   :workflow-name "user_notifications"})

(defn create-tasks
  "Returns workflow tasks"
  []
  (vector (sdk/simple-task (:get-user-info constants) (:get-user-info constants) {:userId "${workflow.input.userId}"})
          (sdk/switch-task "emailorsms" "${workflow.input.notificationPref}" {"email" [(sdk/simple-task (:send-email constants) (:send-email constants) {"email" "${get_user_info.output.email}"})]
                                                                              "sms" [(sdk/simple-task (:send-sms constants) (:send-sms constants) {"phoneNumber" "${get_user_info.output.phoneNumber}"})]} [])))

(defn create-workflow
  "Returns a workflow with tasks"
  [tasks]
  (merge (sdk/workflow (:workflow-name constants) tasks) {:inputParameters ["userId" "notificationPref"]}))

(defn create-workers
  "Returns workers for the workflow"
  []
  [{:name (:get-user-info constants)
    :execute (fn [data]
               {:status "COMPLETED"
                :outputData {"email" (str (get-in data [:inputData "userId"]) "@email.com")}})}

   {:name (:send-email constants)
    :execute (fn [__]
               {:status "COMPLETED"})}

   {:name (:send-sms constants)
    :execute (fn [__]
               {:status "COMPLETED"})}])

(defn create-task-for-name
  "Return a task definition for name"
  [n]
  {:name n,
   :description "some description",
   :ownerEmail "mail@gmail.com",
   :retryCount 3,
   :timeoutSeconds 300,
   :responseTimeoutSeconds 180})

(def workflow-request {:name (:workflow-name constants)
                       :version 1
                       :input {"userId" "jim"
                               "notificationPref" "sms"}})

(defn run [options]
      ;; creates a workflow returns nothing
  (metadata/register-workflow-def options (-> (create-tasks) (create-workflow)) true)
  (metadata/register-tasks options (->> (select-keys constants [:get-user-info :send-email :send-sms]) vals (map #(create-task-for-name %))))

  (let [stop-polling (tr/runner-executer-for-workers options (create-workers))
        async-workflow-id (wr/start-workflow options workflow-request)
        sync-workflow-result (wr/run-workflow-sync options (:workflow-name constants) 1 "requestId" workflow-request)] ;; Runs async workflow

    {:poll-fn stop-polling
     :async-id async-workflow-id
     :sync-result sync-workflow-result}))

(defn check-execution [options {:keys [poll-fn async-id sync-result]}]
  (poll-fn) ;; stop polling
  (let [async-result (wr/get-workflow options async-id)]
    (assert (= (:status async-result) "COMPLETED"))
    (assert (= (:status sync-result) "COMPLETED")))
  (println "Done"))

(defn run-check [options]
  (->> (run options)
       (check-execution options)))

(defn -main
  "runs the example requires app-key app-secret and url"
  [& args]
  (let [[key secret url] args
        options {:app-key key :app-secret secret :url url}]
    (run-check options)))

(comment
  (->> (select-keys constants [:get-user-info :send-email :send-sms]) vals)
  (-> (create-tasks)
      (create-workflow))

  (select-keys constants [:get-user-info :send-email :send-sms])
  (def options
    {:app-key "c38bf576-a208-4c4b-b6d3-bf700b8e454d",
     :app-secret "Z3YUZurKtJ3J9CqrdbRxOyL7kUqLrUGR8sdVknRUAbyGqean",
     :url "http://localhost:8080/api/"})

  (def wf-sample
    {:name "cool_clj_workflow_2",
     :description "created programatically from clj",
     :version 1,
     :tasks [{:name "cool_clj_task_n",
              :taskReferenceName "cool_clj_task_ref",
              :inputParameters {},
              :type "SIMPLE"}
             {:name "something_else",
              :taskReferenceName "other",
              :inputParameters {},
              :type "FORK_JOIN",
              :forkTasks [[{:name "cool_clj_task_n",
                            :taskReferenceName "cool_clj_task_z_ref",
                            :inputParameters {},
                            :type "SIMPLE"}]]}
             {:name "join",
              :type "JOIN",
              :taskReferenceName "join_ref",
              :joinOn ["cool_clj_task_z" "cool_clj_task_x"]}],
     :inputParameters [],
     :outputParameters {"message" "${clj_prog_task_ref.output.:message}"},
     :schemaVersion 2,
     :restartable true,
     :ownerEmail "mail@yahoo.com",
     :timeoutSeconds 0,
     :timeoutPolicy "ALERT_ONLY"})
  (doseq [t (->> (select-keys constants [:get-user-info :send-email :send-sms]) vals)]
    (println {:name t,
              :description "some description",
              :ownerEmail "mail@gmail.com",
              :retryCount 3,
              :timeoutSeconds 300,
              :responseTimeoutSeconds 180}))

  (metadata/register-tasks options {:name "t",
                                    :description "some description",
                                    :ownerEmail "mail@gmail.com",
                                    :retryCount 3,
                                    :timeoutSeconds 300,
                                    :responseTimeoutSeconds 180})

  (def run-res (run-check options))

  ((:poll-fn run-res))

  (def lala (tr/runner-executer-for-workers options (create-workers)))
  (check-execution options run-res)
  ((:poll run-res))
  (println run-res)
  (def worker
    {:name "cool_clj_task_b",
     :execute (fn [d]
                       ;;;; MISISING
                       ;;;;
                       ;; (Thread/sleep 1000)
                (println "lalala " d)
                [:completed d])})

  (tr/runner-executer-for-workers options [worker])

;; => {:name "user_notifications",
;;     :tasks
;;     [{:name "get_user_info",
;;       :taskReferenceName "get_user_info",
;;       :type "SIMPLE",
;;       :inputParameters {:userId "${workflow.input.userId}"}}
;;      {:name "emailorsms",
;;       :taskReferenceName "emailorsms",
;;       :type "SWITCH",
;;       :inputParameters {:switchCaseValue "${workflow.input.notificationPref}"},
;;       :expression "switchCaseValue",
;;       :defaultCase [],
;;       :decisionCases
;;       {"email"
;;        [{:name "send_email",
;;          :taskReferenceName "send_email",
;;          :type "SIMPLE",
;;          :inputParameters {"email" "${get_user_info.output.email}"}}],
;;        "sms"
;;        [{:name "send_sms",
;;          :taskReferenceName "send_sms",
;;          :type "SIMPLE",
;;          :inputParameters
;;          {"phoneNumber" "${get_user_info.output.phoneNumber}"}}]}}],
;;     :version 1,
;;     :inputParameters ["userId" "notificationPref"],
;;     :timeoutSeconds 0}
  )
