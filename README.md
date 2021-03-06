yourcast-bench
==============

##Prerequisites

* java 1.6
* python
* maven

##Note
The following script only work on Unix machine ( Linux and Mac )

##How-TO :

The utilisation is very simple , you just have to clone the repo 

```
git clone https://github.com/fabienfoerster/yourcast-bench.git
```
And you'll have to build the jar to build the program

```
./build_jar
```

### run_experiment

```
./run_experiment --collector CollectMachineIP --scenario ScenarioClassName --output file.xlsx
```
* **--collector** : the IP of the machine where is stock the collected value
* **--scenario** : the className of the gatling scenario located in the **src/test/scala** folder ( gatling-1.5.2)
* **--output** : the name of the excel file you want to store the data 

### extract

```
./extract --collector CollectMachineIP --output file.xlsx --start timestamp --end timstamp
```

* **--collector** : the IP of the machine where is stock the collected value
* **--output** : the name of the excel file you want to store the data 
* **--start** : the timestamp corresponding of the start value in UTC
* **--end** : the timestamp corresponding of the end value in UTC

*(For the timestamp in UTC check : [http://www.epochconverter.com/](http://www.epochconverter.com/))*
