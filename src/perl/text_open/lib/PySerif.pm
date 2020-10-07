use strict;
use warnings FATAL => 'all';
use Carp;

use Utils;
use runjobs4;

package PySerif;


sub new {
    my ($class, %rest) = @_;
    my $self;

    if (%rest) {
        $self = \%rest;
    }
    else {
        $self = {};
    }

    bless $self, $class;

    my @fs = $self->fields();

    # Check the passed fields
    foreach my $k (keys %{$self}) {
        Carp::croak "In new(), $class doesn't have a $k field but was given it as a parameter" unless grep ( /^$k$/, @fs );
    }

    my @missing = grep {!defined($self->{$_})} @fs;
    if (@missing) {
        Carp::croak "In new(), $class not passed mandatory field(s): @missing\n";
    }

    $self->init();

    return $self;
}

sub init {
    my $self = shift;
    $self->{MODULE_NAME} = "PySerif";
    $self->{TEXT_OPEN} = Cwd::abs_path(__FILE__ . "/../../../../../");
    $self->{TEXT_OPEN_PYTHONPATH} = Cwd::abs_path($self->{TEXT_OPEN} . "/src/python");
    $self->{CREATE_FILELIST_PY_PATH} = Cwd::abs_path($self->{TEXT_OPEN_PYTHONPATH} . "/util/common/create_filelist_with_batch_size.py");
}

sub fields {
    return(

    );
}

sub pyserif_pipeline {
    my ($self, %args) = @_;
    my $dependant_job_ids = $args{dependant_job_ids};
    my $job_prefix = $args{job_prefix};
    my $num_batches = $args{num_batches};
    my $input_doc_list = $args{input_doc_list};
    my $PYTHON = $args{PYTHON};
    my $PYTHONPATH = $args{PYTHONPATH};
    my $pipeline_template_path = $args{pipeline_template_path};
    my $pipeline_template_items = $args{pipeline_template_items};
    my $output_dir = $args{output_dir};
    my $schedule_processes = $args{schedule_processes};

    (my $batch_file_dir,my $mkdir_jobid) = Utils::make_output_dir("$output_dir/batch_files","$job_prefix/mkdir-output-dir",$dependant_job_ids);
    my (
        $create_filelist_jobid, @file_list_at_disk
    ) = Utils::split_file_list_with_num_of_batches(
        PYTHON                  => $PYTHON,
        CREATE_FILELIST_PY_PATH => $self->{CREATE_FILELIST_PY_PATH},
        dependant_job_ids       => $mkdir_jobid,
        job_prefix              => $job_prefix . "/create_batch",
        num_of_batches          => $num_batches,
        list_file_path          => $input_doc_list,
        output_file_prefix      => $batch_file_dir . "/batch_",
        suffix                  => ".list",
    );
    my @split_jobs = ();
    my $output_doc_list = "$output_dir/pyserif_files.list";
    for (my $batch = 0; $batch < $num_batches; $batch++) {
        my $batch_file = "$batch_file_dir/batch_$batch.list";
        my $batch_output_folder = "$output_dir/$batch/output";
        my $add_prefix_to_serifxml_in_list =
            runjobs4::runjobs(
                $create_filelist_jobid, "$job_prefix/add_prefix_$batch",
                {
                    BATCH_QUEUE => $self->{LINUX_CPU_QUEUE},
                    SCRIPT      => 1,
                },
                [ "awk '{print \"serifxml\t\"\$0}' $batch_file > $batch_file\.with_type" ]
            );

        my $pyserif_dep_jobid;
        if ($schedule_processes) 
        { 
            $pyserif_dep_jobid = runjobs4::runjobs(
                [ $add_prefix_to_serifxml_in_list ], "$job_prefix/pyserif_$batch",
                $pipeline_template_items,
                [ "mkdir -p $batch_output_folder" ],
                [ "env PYTHONPATH=$PYTHONPATH:$self->{TEXT_OPEN_PYTHONPATH} " .
                    "KERAS_BACKEND=\"tensorflow\" " .
                    "OMP_NUM_THREADS=1 " .
                    "$PYTHON $self->{TEXT_OPEN_PYTHONPATH}/serif/driver/pipeline.py", $pipeline_template_path, "SCHEDULE_PROCESSES $batch_file\.with_type $batch_output_folder" ]
            );
        } 
        else
        { 
            $pyserif_dep_jobid = runjobs4::runjobs(
                [ $add_prefix_to_serifxml_in_list ], "$job_prefix/pyserif_$batch",
                $pipeline_template_items,
                [ "mkdir -p $batch_output_folder" ],
                [ "env PYTHONPATH=$PYTHONPATH:$self->{TEXT_OPEN_PYTHONPATH} " .
                    "KERAS_BACKEND=\"tensorflow\" " .
                    "$PYTHON $self->{TEXT_OPEN_PYTHONPATH}/serif/driver/pipeline.py", $pipeline_template_path, "$batch_file\.with_type $batch_output_folder" ]
            );
        }  
  
        push(@split_jobs, $pyserif_dep_jobid);
    }
    my $list_collector_job_id =
        runjobs4::runjobs(
            \@split_jobs,
            "$job_prefix/" .
                "list_collector",
            {
                BATCH_QUEUE => $self->{LINUX_CPU_QUEUE},
                SCRIPT      => 1
            },
            [
                "find $output_dir -name \"*.xml\" -exec readlink -f {} \\;  " .
                    " | sort -u > $output_doc_list"
            ]
        );
    return([ $list_collector_job_id ], $output_doc_list);
}

1;
