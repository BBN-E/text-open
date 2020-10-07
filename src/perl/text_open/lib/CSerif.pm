use strict;
use warnings FATAL => 'all';
use runjobs4;

use lib::Utils;

package CSerif;

# sub CSerifBatch {
#     my @previous_task = @{$_[0]};
#     my $params = $_[1];
#
#     my $processing_dir = Utils::get_param($params, "processing_dir");
#     my $output_list_path = Utils::get_param($params, "output_list_path");
#
#
#     my $stage_name = "serif";
#     my $batch_file_dir = Utils::make_output_dir("$processing_dir/$stage_name/batch_files");
#     my $stage_output_folder = Utils::make_output_dir("$processing_dir/$stage_name");
#     my ($NUM_JOBS, $split_serif_jobid) = Utils::split_file_for_processing("$JOB_NAME/$stage_name/make_serif_batch_files", $input_sgm_list, "$batch_file_dir/", $BATCH_SIZE);
# }

sub CSerif {
    my @previous_task = @{$_[0]};
    my $params = $_[1];
    my @serif_jobs = ();
    my $serif_jobid =
        runjobs4::runjobs(
            \@previous_task, Utils::get_param($params, "job_name"),
            {
                par_dir                          => constants::get_par()->{"project_specific_deps"}->{"CSERIF"}->{"par_dir"},
                experiment_dir                   => Utils::get_param($params, "batch_output_dir"),
                batch_file                       => Utils::get_param($params, "batch_file"),
                icews_lib_dir                    => Utils::get_param(constants::get_par()->{"project_specific_deps"}->{"CSERIF"}, "icews_lib_dir", "None"),
                bbn_actor_db                     => Utils::get_param($params, "awake_db"),
                project_specific_serif_data_root => Utils::get_param($params, "project_specific_serif_data_root"),
                cause_effect_output_dir          => Utils::get_param($params, "serif_cause_effect_output_dir"),
                SERIF_DATA                       => constants::get_par()->{"project_specific_deps"}->{"CSERIF"}->{"SERIF_DATA"},
                SGE_VIRTUAL_FREE                 => Utils::get_param($params, "SGE_VIRTUAL_FREE", "16G"),
                BATCH_QUEUE                      => Utils::get_param($params, "LINUX_QUEUE", "NON_EXISTING_QUEUE"),
                serif_cause_effect_patterns_dir  => Utils::get_param($params, "serif_cause_effect_patterns_dir"),
                should_track_files_read          => Utils::get_param($params, "should_track_files_read", "false"),
                use_basic_cipher_stream          => Utils::get_param($params, "use_basic_cipher_stream", "false")
            },
            [ constants::get_par()->{"project_specific_deps"}->{"CSERIF"}->{"CSERIF_EXE"}, Utils::get_param($params, "project_specific_serif_par") ]
        );
    push(@serif_jobs, $serif_jobid);
    return @serif_jobs;
}

sub merge_fact_finder {
    my @previous_task = @{$_[0]};
    my $params = $_[1];
    return runjobs4::runjobs(
        \@previous_task, Utils::get_param($params, "job_name"),
        {
            BATCH_QUEUE => Utils::get_param($params, "LINUX_QUEUE", "NON_EXISTING_QUEUE"),
            SCRIPT      => 1
        },
        [ constants::get_par()->{"binaries"}->{"PYTHON3"} . " " . constants::get_par()->{"project_specific_deps"}->{"CSERIF"}->{"FACTFINDER_TO_JSON_SCRIPT"} . " " . Utils::get_param($params, "stage_output_folder") . " " . Utils::get_param($params, "factfinder_output_path") ]
    );
}

1;