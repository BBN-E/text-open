# Read in serifxml then save it an make sure the files are 
# essentially identical

import sys, os
script_dir = os.path.dirname(os.path.realpath(__file__))
sys.path.append(os.path.join(script_dir, ".."))
import serifxml3

if len(sys.argv) != 3:
    print("Usage: " + sys.argv[0] + " input-serifxml-file output-serifxml-file")
    sys.exit(1)

input_file, output_file = sys.argv[1:]

if os.path.exists(output_file):
    os.remove(output_file)

doc = serifxml3.Document(input_file)
doc.save(output_file)

print("Reading input serifxml")
i = open(input_file)
print("Writing output serifxml")
o = open(output_file)

i_contents = i.read()
o_contents = o.read()

i.close()
o.close()

print("Checking")
if i_contents.strip() != o_contents.strip():
    print("Serifxml files differ")
    sys.exit(1)

print("Serifxml files match")
