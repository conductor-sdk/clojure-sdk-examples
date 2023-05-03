# Clojure QuickStart Example
This repository contains sample applications that demonstrates the various features of Conductor Clojure SDK.

## SDK Features
Clojure SDK for Conductor allows you to:
1. Create workflow using Code
2. Execute workflows
3. Create workers for task execution and framework for executing workers and communicating with the server.
4. Support for all the APIs such as
    1. Managing tasks (poll, update etc.),
    2. Managing workflows (start, pause, resume, terminate, get status, search etc.)
    3. Create and update workflow and task metadata

### Running Example

> **Note**
Obtain KEY and SECRET from the playground or your Conductor server. [Quick tutorial for playground](https://orkes.io/content/docs/getting-started/concepts/access-control-applications#access-keys)


Run the main program
```shell
clj -X:run-x :app-key '"someKey"' :app-secret '"someSecret"' :url '"http://yourEnv.com"'
```

## Workflow

We create a simple 2-step workflow that fetches the user details and sends an email.

<table><tr><th>Visual</th><th>Code</th></tr>
<tr>
<td width="50%"><img src="resources/workflow.png" width="250px"></td>
<td>
<pre>

``` clojure

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
  
(-> (create-tasks) (create-workflow))
```

                                                                                

</pre>
</td>
</tr>
</table>

## Worker

Workers are implemented as simple functions with sample implementation.  

``` clojure
[{:name (:get-user-info constants)
    :execute (fn [data]
               [:completed {"email" (str (get-in data [:inputData "userId"]) "@email.com")}])}

   {:name (:send-email constants)
    :execute (fn [__]
               [:completed {}])}

   {:name (:send-sms constants)
    :execute (fn [__]
               [:completed {}])}]
```


## Executing Workflows

There are two ways to execute a workflow:

1. Synchronously - useful for short duration workflows that completes within a few second.
2. Asynchronously - workflows that runs for longer period

### Synchronous Workflow Execution
```clojure
(wr/run-workflow-sync options "workflowName" 1 "ArequestId" workflow-request)
```

### Asynchronous Workflow Execution

```clojure
(wr/start-workflow options workflow-request)
```

See [clojure_sdk_examples.clj](src/io_orkes/clojure_sdk_examples.clj) for complete code sample of workflow execution.
