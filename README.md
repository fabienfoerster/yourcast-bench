yourcast-bench
==============

##Prerequisites

You need to be sure you have python and java installed on your machine 

##Note
The following script only work on Unix machine ( Linux and Mac )

##How-TO :

The utilisation is very simple , you just have to clone the repo 

```
git clone https://github.com/fabienfoerster/yourcast-bench.git
```
### run_experiment

```
./run_experiment --collector CollectMachine --scenario ScenarioClassName --output file.xlsx
```
* **--collector** : the IP of the machine where is stock the collected value
* **--scenario** : the className of the gatling scenario located in the simulation folder
* **--output** : the name of the excel file you want to store the data 

### extract

```
./extract --collector CollectMachine --output file.xlsx --start timestamp --end timstamp
```

* **--collector** : the IP of the machine where is stock the collected value
* **--output** : the name of the excel file you want to store the data 
* **--start** : the timestamp corresponding of the start value in UTC
* **--end** : the timestamp corresponding of the end value in UTC
