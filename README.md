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

