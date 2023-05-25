#!/usr/bin/env perl
use strict;
use warnings FATAL => 'all';

use FindBin qw($Bin $Script);

# Load MT-style defines file
my $p;
BEGIN {
    (my $param_file = "$Bin/$Script") =~ s/\.pl$/.defines.pl/;
    my $user_params = do $param_file;
    if (!defined $user_params) {
        die "Unable to parse '$param_file': $@\n" if $@;
        die "Unable to load '$param_file': $!\n" if $!;
    }
    $p = $user_params;
    unshift @INC, @{$p->{perl_libs}};
}

use runjobs4;
use Utils;

my $last_jobs_ptr = [];
my %stages = map {$_ => 1} @{$p->{stages_to_run}};

my ($exp_dir, $exp) = startjobs(%{$p->{runjobs_pars}});
max_jobs($p->{max_jobs});
my $expts = "$exp_dir/expts/$p->{job_prefix}";
my $use_gpus = $p->{use_gpus};
{
    my $stage_name = "simple_map";
    my $job_prefix = $p->{job_prefix} . "/$stage_name";
    my $stage_processing_dir = "$expts/$stage_name";

    foreach my $lang_entry (@{$p->{language_input_entries}}) {
        my $lang_list = $lang_entry->{list};
        my $lang_par = $lang_entry->{par};
        my $language_name = $lang_entry->{lang};
        my $number_of_batches = $lang_entry->{num_of_batches} || $p->{num_of_batches};

        my $batch_file_dir = "$stage_processing_dir/$language_name/batch/batch";
        my $job_output_dir = "$stage_processing_dir/$language_name/output";
        my $language_job_prefix = "$job_prefix/$language_name";
        # Split

        my ($split_jobid, @batch_files) = Utils::split_file_list_with_num_of_batches(
            PYTHON                  => $p->{PYTHON3_SYSTEM},
            CREATE_FILELIST_PY_PATH => "$p->{TEXT_OPEN_PYTHONPATH}/util/common/create_filelist_with_batch_size.py",
            num_of_batches          => $number_of_batches,
            suffix                  => "",
            output_file_prefix      => $batch_file_dir,
            list_file_path          => $lang_list,
            job_prefix              => "$language_job_prefix/",
            dependant_job_ids       => $last_jobs_ptr,
        );
        # Map
        my $main_python = $p->{PYTHON_CPU};
        my $main_queue = $p->{runjobs_pars}->{batch_queue};
        if($use_gpus){
            $main_python = $p->{PYTHON_GPU};
            $main_queue = $p->{runjobs_pars}->{batch_gpu_queue};
        }
        for (my $n = 0; $n < $number_of_batches; $n++) {
            my $batch_job_name = "$language_job_prefix/split/$n";
            my $job_batch_num = $n;
            my $batch_file = $batch_files[$n];
            my $batch_job_output_dir = "$job_output_dir/$job_batch_num";
            my $batch_jobid = runjobs($split_jobid, $batch_job_name, {
                BATCH_QUEUE          => $main_queue,
                TEXT_OPEN_PYTHONPATH => $p->{TEXT_OPEN_PYTHONPATH},
                LANGUAGE_MODELS_DIR  => $p->{LANGUAGE_MODELS_DIR}
            }, [ "$main_python $p->{TEXT_OPEN_PYTHONPATH}/serif/driver/pipeline.py", $lang_par, "$batch_file $batch_job_output_dir" ],
                [ "find $batch_job_output_dir -name \"*.xml\" -exec readlink -f {} \\;  " .
                    "| sort -u > $batch_job_output_dir/serif.list" ]
            );
        }
    }
}

endjobs();

1;
