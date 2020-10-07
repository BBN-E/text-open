#!/usr/bin/perl
use strict;
use warnings FATAL => 'all';
# This should be used at first, due to we want to use a new enough runjobs4
use FindBin qw($Bin $Script);
use Cwd;

use lib Cwd::abs_path(__FILE__ . "/../");
use lib::constants;
use lib::CSerif;
use lib::Utils;

package main;

use runjobs4;

my $JOB_NAME = "serif_test_drive";
my $BATCH_SIZE = 20;
my ($exp_root, $exp) = runjobs4::startjobs("queue_mem_limit" => '8G', "max_memory_over" => '0.5G');

my $processing_dir = Utils::make_output_dir("$exp_root/expts/$JOB_NAME");
constants::assemble_par({
    "project_roots" =>{
        "hume"=>"/home/hqiu/ld100/Hume_pipeline/Hume"
    }
});


my $GENERATED_SERIF_SERIFXML = "$processing_dir/serif_serifxml.list";
my $GENERATED_FACTFINDER_JSON_FILE = "$processing_dir/serif/facts.json";
my $GENERATED_SERIF_CAUSE_EFFECT_JSON_DIR = "$processing_dir/serif_cause_effect_json";
if(1){
    print "Serif stage\n";
    my $input_sgm_list = "/home/hqiu/tmp/ben_sentence.list";
    my $awake_db = "/nfs/raid87/u14/CauseEx/Kaliningrad-Dataset/TwoSix-Extracted-M15-Assessment/awake_dbs/causeex_dbpedia_20170308_m15a_kaliningrad_fix.sqlite";

    my $mode = "WorldModelers";
    my $hume_repo_root = constants::get_par()->{"project_roots"}->{"hume"};
    my $serif_cause_effect_patterns_dir = "$hume_repo_root/resource/serif_cause_effect_patterns";
    my $project_specific_serif_data_root = "$hume_repo_root/resource/serif_data_wm";
    my $project_specific_serif_par = "$hume_repo_root/templates/serif_wm.par";
    my $serif_server_mode_endpoint = "None";


    my $stage_name = "serif";
    my $stage_output_folder = Utils::make_output_dir("$processing_dir/$stage_name");
    my $batch_file_dir = Utils::make_output_dir("$processing_dir/$stage_name/batch_files");
    my ($NUM_JOBS, $split_serif_jobid) = Utils::split_file_for_processing("$JOB_NAME/$stage_name/make_serif_batch_files", $input_sgm_list, "$batch_file_dir/", $BATCH_SIZE);
    my @serif_jobs = ();
    for (my $n = 0; $n < $NUM_JOBS; $n++) {
        my $job_batch_num = sprintf("%05d", $n);
        my $serif_job_name = "$JOB_NAME/$stage_name/$job_batch_num";
        my $experiment_dir = "$processing_dir/$stage_name/$job_batch_num";
        my $batch_file = "$batch_file_dir/$job_batch_num";
        my @serif_jobs_in_batch = CSerif::CSerif(
            [ $split_serif_jobid ],
            {
                project_specific_serif_par       => $project_specific_serif_par,
                batch_file                       => $batch_file,
                batch_output_dir                 => $experiment_dir,
                awake_db                         => $awake_db,
                job_name                         => $serif_job_name,
                serif_cause_effect_patterns_dir  => $serif_cause_effect_patterns_dir,
                project_specific_serif_data_root => $project_specific_serif_data_root,
                LINUX_QUEUE                      => "nongale-sl6",
                serif_cause_effect_output_dir    => $GENERATED_SERIF_CAUSE_EFFECT_JSON_DIR
            }
        );
        for my $serif_job (@serif_jobs_in_batch) {
            push(@serif_jobs, $serif_job);
        }
    }
    if (($serif_server_mode_endpoint eq "None") and ($mode eq "CauseEx")) {

        CSerif::merge_fact_finder(\@serif_jobs,{
            job_name               => "$JOB_NAME/$stage_name/process_factfinder_results",
            LINUX_QUEUE            => "nongale-sl6",
            factfinder_output_path => $GENERATED_FACTFINDER_JSON_FILE,
            stage_output_folder    => $stage_output_folder
        });
    }
    else {
        # @hqiu: Logic here is convoluted !!!
        Utils::make_output_dir($GENERATED_FACTFINDER_JSON_FILE);
    }
    my $generate_serif_output_list = Utils::generate_file_list(\@serif_jobs, "$JOB_NAME/$stage_name/generate_file_list", "$processing_dir/$stage_name/*/output/*.xml", $GENERATED_SERIF_SERIFXML);
}
runjobs4::dojobs();

runjobs4::endjobs();
1;