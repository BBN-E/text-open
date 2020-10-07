
import time,os,sys,shutil,platform,logging

current_script_path = __file__
project_root = os.path.realpath(os.path.join(current_script_path, os.path.pardir))
sys.path.append(project_root)

from serif.viewer import write_all_pages

logger = logging.getLogger(__name__)

def is_exe(fpath):  # Is the given path an executable?
    return os.path.isfile(fpath) and os.access(fpath, os.X_OK)


def which(program):  # Can I find the named binary in the path?
    fpath, _fname = os.path.split(program)
    if fpath:
        if is_exe(program):
            return program
    else:
        for path in os.environ["PATH"].split(os.pathsep):
            path = path.strip('"')
            exe_file = os.path.join(path, program)
            if is_exe(exe_file):
                return exe_file
    return None

def assign_html_filenames(inputs, parser):
    html_files = {}
    used_html_names = set()
    for filename in inputs:
        if not os.path.exists(filename):
            parser.error("File %s not found" % filename)
        html_file = os.path.split(filename)[-1] + '.html'
        if html_file in used_html_names:
            n = 2
            while ('%s-%s.html' % (html_file[:-4], n)) in used_html_names:
                n += 1
            html_file = '%s-%s.html' % (html_file[:-4], n)
        html_files[filename] = html_file
    return html_files


###########################################################################
## Command-line Interface
###########################################################################

USAGE = """\
Usage: %prog [options] INPUTS -o OUTPUT_DIR

Render each input file as an HTML file.  Use -h to see options.
"""


def cli():
    import optparse

    parser = optparse.OptionParser(usage=USAGE)
    parser.add_option("-o", "--out-dir", dest="out_dir", metavar='DIR',
                      help='Directory where output should be written [REQUIRED]')
    parser.add_option("--nav-only", dest="nav_only",
                      action='store_true', default=False,
                      help='Only generate the navigation index page, '
                           'and not the document pages.')
    parser.add_option("-f", dest="force", action='store_true', default=False,
                      help='Rebuild all output files, even if an existing '
                           'output file is newer than the input file.')
    parser.add_option("-p", "--path", dest="path", default='',
                      help='Path to Graphviz binaries')
    parser.add_option("--no-doc-pages", dest='show_doc_pages',
                      action='store_false', default=True,
                      help='Do not generate the document-view pages.  This '
                           'option is usually used in conjunction with another '
                           'option (such as --find-terms) that displays other '
                           'pages.')
    parser.add_option("--no-doc-entities", dest='show_doc_entities',
                      action='store_false', default=True,
                      help='Do not generate html for entities at the doc level.')
    parser.add_option("--no-doc-graphs", dest='show_doc_graphs',
                      action='store_false', default=True,
                      help='Do not generate the graphs at the doc level.')
    parser.add_option("--no-merged-graphs", dest='show_merged_graphs',
                      action='store_false', default=True,
                      help='Do not generate the merged graphs.')
    parser.add_option("--find-terms", metavar='FILE',
                      action='append', dest='term_search_files',
                      help='Run a text search for all of the patterns that '
                           'are listed in the specified file.')
    parser.add_option("--find-props", metavar='FILE',
                      action='append', dest='prop_search_files',
                      help='Run a prop search for all of the patterns that '
                           'are listed in the specified file.')
    parser.add_option("--index-per-file", action="store_true", dest="index_per_file",
                      default=False, help='Create a separate directory and  '
                                          'index for each file. If this is specified, there must '
                                          'be a single input file with two tab-separated columns: the directory '
                                          'name first (typically a document ID) and the SerifXML '
                                          'file to view second')
    parser.add_option("--slice", metavar='NUMBER', dest='sliceNum', default=0,
                      help='Slice of file list to render.  Requires '
                           '--index-per-file and --parallel. Must be >= 0 and < --parallel. '
                           'Typically used for runjobs scripts')
    parser.add_option('--parallel', metavar='NUMBER', dest='parallel', default=1,
                      help='Number of parallel slices.  Must be >= 0. Requires '
                           '--index-per-file and --slice. Typically used for runjobs scripts')

    (options, inputs) = parser.parse_args()
    if options.path:  # Extend our path if need be
        os.environ["PATH"] += os.pathsep + options.path

    # Check if we can find the dot and fdp GraphViz binaries
    if platform.system() == 'Windows':
        if not (which('dot.exe') and which('fdp.exe')):
            sys.exit(
                "Could not find GraphViz' dot.exe and fdp.exe in the path.  Please install GraphViz and set your path accordingly.")
    else:
        if not (which('dot') and which('fdp')):
            sys.exit(
                "Could not find GraphViz' dot and fdp in the path.  Please install Graphviz and set your path accordingly.")

    if not inputs:
        parser.error("Expected at least one input file")
    inputs = [input for input in inputs]
    out_dir = options.out_dir
    if not out_dir:
        parser.error("Output directory is required.")

    # Find our resource directory.
    data_dir = os.path.abspath(os.path.join(project_root,'serif','viewer',
                                            'serifxml_viewer_resources'))
    if not os.path.exists(data_dir):
        parser.error("Unable to find serifxml_viewer_resources directory; "
                     "expected to find it in %r" % data_dir)

    if options.index_per_file:
        run_with_index_per_file(inputs, parser, data_dir, out_dir, options)
    else:
        run_with_single_index(inputs, parser, data_dir, out_dir, options)


def run_with_single_index(inputs, parser, data_dir, out_dir, options):
    # If the input is a directory, use the files in it as inputs
    if len(inputs) == 1 and os.path.isdir(inputs[0]):
        input_dir = inputs[0]
        inputs = [os.path.join(input_dir, f) for f in os.listdir(input_dir) if
                  os.path.isfile(os.path.join(input_dir, f))]

    # Pick a unique html filename for each input file
    html_files = assign_html_filenames(inputs, parser)

    # Create the output dir if it doesn't exist.
    os.makedirs(out_dir,exist_ok=True)

    # Copy css/javascript files to the output dir
    shutil.copy(os.path.join(data_dir, 'serifxml_viewer.css'),
                os.path.join(out_dir, 'serifxml_viewer.css'))
    shutil.copy(os.path.join(data_dir, 'serifxml_viewer.js'),
                os.path.join(out_dir, 'serifxml_viewer.js'))

    # and the jquery and jstree code to avoid web-based losage
    shutil.copy(os.path.join(data_dir, 'jquery-1.8.2.min.js'),
                os.path.join(out_dir, 'jquery.js'))
    shutil.copy(os.path.join(data_dir, 'jquery.jstree.js'),
                os.path.join(out_dir, 'jstree.js'))

    write_all_pages(html_files, out_dir, options)


def run_with_index_per_file(inputs, parser, data_dir, out_dir, options):
    if len(inputs) != 1 or not os.path.isfile(inputs[0]):
        sys.exit(
            "If --index-per-file is specified, must have a single input file, a file mapping document IDs to SerifXML files")

    os.makedirs(out_dir,exist_ok=True)

    sliceNum = int(options.sliceNum)
    parallel = int(options.parallel)
    count = 0
    for (docID, serifXMLFile) in parseFileMap(inputs[0]):
        if count % parallel == sliceNum:
            documentDir = os.path.join(out_dir, docID)
            run_with_single_index([serifXMLFile], parser, data_dir, documentDir, options)
        count += 1


def parseFileMap(f):
    ret = []
    for line in open(f):
        ret.append(line.strip().split('\t'))
    return ret

if __name__ == '__main__':
    try:
        logging.basicConfig(level=logging.getLevelName(os.environ.get('LOGLEVEL', 'INFO').upper()))
    except ValueError as e:
        logging.error("Unparseable level {}, will use default {}.".format(os.environ.get('LOGLEVEL', 'INFO').upper(),logging.root.level))
    start_time = time.time()
    cli()
    end_time = time.time()
    logger.info("Program execution took %.2f seconds" % (end_time - start_time))