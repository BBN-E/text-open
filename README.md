Java and Python APIs for reading and writing SERIF XMLs

# Using the Driver

#### From sgm to serifxml(pure writing)

```
cd src/python
python3 serif/driver/pipeline.py config/config_sgm_to_end test/rundir/output/input_list_bmin.txt test/tmp_out
```

#### From serifxml to serifxml(read and write)

```
cd src/python
python3 serif/driver/pipeline.py config/config test/rundir/output/input_list.txt test/tmp_out
```

### Acknowledgments

This work was supported by DARPA/I2O and U.S. Air Force Research Laboratory Contract No. FA8650-17-C-7716 under the Causal Exploration program, and DARPA/I2O and U.S. Army Research Office Contract No. W911NF-18-C-0003 under the World Modelers program. The views, opinions, and/or findings expressed are those of the author(s) and should not be interpreted as representing the official views or policies of the Department of Defense or the U.S. Government. This document does not contain technology or technical data controlled under either the U.S. International Traffic in Arms Regulations or the U.S. Export Administration Regulations.
