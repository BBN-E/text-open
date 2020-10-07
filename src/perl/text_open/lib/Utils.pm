use strict;
use warnings FATAL => 'all';
use Cwd;
use File::Path;
use runjobs4;
use Data::Dumper;
package Utils;

sub load_params {
    my %params = ();
    my $config_file = $_[0];

    open(my $fh, '<', $config_file) or die "Could not open config file: $config_file";
    while (my $line = <$fh>) {
        $line =~ s/^\s+|\s+$//g;
        next if length($line) == 0;
        next if substr($line, 0, 1) eq '#';
        my @pieces = split(/:/, $line, 2);
        if (scalar(@pieces) != 2) {
            die "Could not find key: value pair in config file line: $line\n";
        }
        my $param_name = $pieces[0];
        my $param_value = $pieces[1];
        $param_name =~ s/^\s+|\s+$//g;
        $param_value =~ s/^\s+|\s+$//g;
        $params{$param_name} = $param_value;
    }

    close($fh);

    return \%params;
}

sub get_param {
    my $params_ref = $_[0];
    my $param_key = $_[1];
    my $default_value;

    if (scalar(@_) > 2) {
        $default_value = $_[2];
    }

    if (!defined($params_ref->{$param_key})) {
        if (defined($default_value)) {
            return $default_value;
        }
        else {
            die "Required parameter: $param_key not set";
        }
    }

    return $params_ref->{$param_key};
}



sub split_file_list_with_num_of_batches {
    my %args = @_;
    my $PYTHON = $args{PYTHON};
    my $CREATE_FILELIST_PY_PATH = $args{CREATE_FILELIST_PY_PATH};
    my $dependant_job_ids = $args{dependant_job_ids};
    my $num_of_batches = $args{num_of_batches};
    my $job_prefix = $args{job_prefix};
    my $list_file_path = $args{list_file_path};
    my $output_file_prefix = $args{output_file_prefix};
    my $suffix = $args{suffix};
    my $create_filelist_jobid = runjobs4::runjobs(
        $dependant_job_ids, $job_prefix . "split_filelist_with_num_of_batches_" . $num_of_batches,
        {
            SCRIPT => 1
        },
        [ "mkdir -p `dirname $output_file_prefix`"],
        [ "$PYTHON $CREATE_FILELIST_PY_PATH --list_file_path \"$list_file_path\" --output_list_prefix \"$output_file_prefix\" --num_of_batches $num_of_batches --suffix \"$suffix\"" ],
    );
    my @file_list_at_disk = ();
    for (my $i = 0; $i < $num_of_batches; ++$i) {
        push(@file_list_at_disk, $output_file_prefix . $i . $suffix);
    }
    return([ $create_filelist_jobid ], @file_list_at_disk);
}

sub split_file_for_processing {
    my $split_jobname = $_[0];
    my $bf = $_[1];
    my $bp = $_[2];
    my $bs = $_[3];

    open my $fh, "<", $bf or die "could not open $bf: $!";
    my $num_files = 0;
    $num_files++ while <$fh>;
    my $njobs = int($num_files / $bs) + 1;
    if ($num_files % $bs == 0) {
        $njobs--;
    }
    print "File $bf will be broken into $njobs batches of approximate size $bs\n";
    my $jobid = runjobs4::runjobs([], "$split_jobname",
        {
            SCRIPT => 1,
        },
        "/usr/bin/split -d -a 5 -l $bs $bf $bp");

    return($njobs, $jobid);
}

sub make_output_dir {
    my $dir = $_[0];
    my $job_name = $_[1];
    my $job_depdendencies = $_[2];
    my $job_id = runjobs4::runjobs(
        $job_depdendencies, $job_name,
        {
            SCRIPT => 1
        },
        [ "mkdir -p $dir" ]
    );
    return $dir,[$job_id];
}

1;
