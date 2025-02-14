## Community edition (free-tier)

You just need to install the Sedona jars and Sedona Python on Databricks using Databricks default web UI. Then everything will work.

## Advanced editions

We recommend Databricks 10.x+.

!!!tip
	Wherobots Cloud provides a free tool to deploy Apache Sedona to Databricks. Please sign up [here](https://www.wherobots.services/).

* Sedona 1.0.1 & 1.1.0 is compiled against Spark 3.1 (~ Databricks DBR 9 LTS, DBR 7 is Spark 3.0)
* Sedona 1.1.1, 1.2.0 are compiled against Spark 3.2 (~ DBR 10 & 11)
* Sedona 1.2.1, 1.3.1, 1.4.0 are complied against Spark 3.3
* 1.4.1, 1.5.0 are complied against Spark 3.3 and 3.4

> In Spark 3.2, `org.apache.spark.sql.catalyst.expressions.Generator` class added a field `nodePatterns`. Any SQL functions that rely on Generator class may have issues if compiled for a runtime with a differing spark version. For Sedona, those functions are:
>    * ST_MakeValid
>    * ST_SubDivideExplode

!!!note
	If you are using Spark 3.4+ and Scala 2.12, please use `sedona-spark-shaded-3.4_2.12`. Please pay attention to the Spark version postfix and Scala version postfix. Sedona is not able to support `Databricks photon acceleration`. Sedona requires Spark internal APIs to inject many optimization strategies, which is not accessible in `Photon`.

## Install Sedona from the web UI (not recommended)

This method cannot achieve the best performance of Sedona and does not work for pure SQL environment.

### Install libraries

1) From the Libraries tab install from Maven Coordinates
    ```
    org.apache.sedona:sedona-spark-shaded-3.0_2.12:{{ sedona.current_version }}
    org.datasyslab:geotools-wrapper:{{ sedona.current_geotools }}
    ```

2) For enabling python support, from the Libraries tab install from PyPI
    ```
    apache-sedona
    keplergl==0.3.2
    pydeck==0.8.0
    ```

3) (Only for DBR up to 7.3 LTS) You can speed up the serialization of geometry types by adding to your spark configurations (`Cluster` -> `Edit` -> `Configuration` -> `Advanced options`) the following lines:
    ```
    spark.serializer org.apache.spark.serializer.KryoSerializer
    spark.kryo.registrator org.apache.sedona.core.serde.SedonaKryoRegistrator
    ```

### Initialize

After you have installed the libraries and started the cluster, you can initialize the Sedona `ST_*` functions and types by running from your code:

(scala)
```scala
import org.apache.sedona.sql.utils.SedonaSQLRegistrator
SedonaSQLRegistrator.registerAll(spark)
```

(or python)
```python
from sedona.register.geo_registrator import SedonaRegistrator
SedonaRegistrator.registerAll(spark)
```

## Install Sedona from the init script

In order to activate the Kryo serializer (this speeds up the serialization and deserialization of geometry types) you need to install the libraries via init script as described below.

In order to use the Sedona `ST_*` functions from SQL without having to register the Sedona functions from a python/scala cell, you need to install the Sedona libraries from the [cluster init-scripts](https://docs.databricks.com/clusters/init-scripts.html) as follows.

### Download Sedona jars

Download the Sedona jars to a DBFS location. You can do that manually via UI or from a notebook by executing this code in a cell:

```bash
%sh
# Create JAR directory for Sedona
mkdir -p /dbfs/FileStore/jars/sedona/{{ sedona.current_version }}

# Download the dependencies from Maven into DBFS
curl -o /dbfs/FileStore/jars/sedona/{{ sedona.current_version }}/geotools-wrapper-{{ sedona.current_geotools }}.jar "https://repo1.maven.org/maven2/org/datasyslab/geotools-wrapper/{{ sedona.current_geotools }}/geotools-wrapper-{{ sedona.current_geotools }}.jar"

curl -o /dbfs/FileStore/jars/sedona/{{ sedona.current_version }}/sedona-spark-shaded-3.0_2.12-{{ sedona.current_version }}.jar "https://repo1.maven.org/maven2/org/apache/sedona/sedona-spark-shaded-3.0_2.12/{{ sedona.current_version }}/sedona-spark-shaded-3.0_2.12-{{ sedona.current_version }}.jar"
```

### Create an init script

Create an init script in DBFS that loads the Sedona jars into the cluster's default jar directory. You can create that from any notebook by running:

```bash
%sh

# Create init script directory for Sedona
mkdir -p /dbfs/FileStore/sedona/

# Create init script
cat > /dbfs/FileStore/sedona/sedona-init.sh <<'EOF'
#!/bin/bash
#
# File: sedona-init.sh
#
# On cluster startup, this script will copy the Sedona jars to the cluster's default jar directory.
# In order to activate Sedona functions, remember to add to your spark configuration the Sedona extensions: "spark.sql.extensions org.apache.sedona.viz.sql.SedonaVizExtensions,org.apache.sedona.sql.SedonaSqlExtensions"

cp /dbfs/FileStore/jars/sedona/{{ sedona.current_version }}/*.jar /databricks/jars

EOF
```

### Set up cluster config

From your cluster configuration (`Cluster` -> `Edit` -> `Configuration` -> `Advanced options` -> `Spark`) activate the Sedona functions and the kryo serializer by adding to the Spark Config
```
spark.sql.extensions org.apache.sedona.viz.sql.SedonaVizExtensions,org.apache.sedona.sql.SedonaSqlExtensions
spark.serializer org.apache.spark.serializer.KryoSerializer
spark.kryo.registrator org.apache.sedona.core.serde.SedonaKryoRegistrator
```

From your cluster configuration (`Cluster` -> `Edit` -> `Configuration` -> `Advanced options` -> `Init Scripts`) add the newly created init script
```
dbfs:/FileStore/sedona/sedona-init.sh
```

For enabling python support, from the Libraries tab install from PyPI
```
apache-sedona
keplergl==0.3.2
pydeck==0.8.0
```

!!!tips
	You need to install the Sedona libraries via init script because the libraries installed via UI are installed after the cluster has already started, and therefore the classes specified by the config `spark.sql.extensions`, `spark.serializer`, and `spark.kryo.registrator` are not available at startup time.*
