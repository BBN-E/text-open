#!/usr/bin/env perl
use strict;
use warnings FATAL => 'all';

my $RUNJOBS_RELEASE_DIR = "/d4m/ears/releases/runjobs4/TMP/TMP2021_01_11.65e0fcb.set-gpus-on-gpu-queue-in-docker";
my $TEXT_OPEN_RELEASE_DIR = "/nfs/raid88/u10/users/hqiu_ad/repos/text-open";

my $TEXT_OPEN_PYTHONPATH = "$TEXT_OPEN_RELEASE_DIR/src/python/";

my $PRODUCTION_MODE = defined $ENV{'PRODUCTION_MODE'} ? $ENV{'PRODUCTION_MODE'} : 'false';

my $LANGUAGE_MODELS_DIR = "/nfs/raid87/u10/nlp";

my $PYTHON_GPU = "/d4m/material/software/python/singularity/bin/singularity-python.sh -i python3.6-cuda10.0 -v /nfs/raid84/u11/material/software/python/singularity/.venv.stage/python3.6-cuda10.0/pyserif-gpu -e KERAS_BACKEND=tensorflow -e PRODUCTION_MODE=$PRODUCTION_MODE --gpu -l $TEXT_OPEN_PYTHONPATH:$LANGUAGE_MODELS_DIR/spacy:/nfs/raid66/u11/users/brozonoy-ad/modal_and_temporal_parsing";

my $PYTHON_CPU = "/d4m/material/software/python/singularity/bin/singularity-python.sh -i python3.6-cuda10.0 -v /nfs/raid84/u11/material/software/python/singularity/.venv.stage/python3.6-cuda10.0/pyserif-cpu -e PRODUCTION_MODE=$PRODUCTION_MODE -l $TEXT_OPEN_PYTHONPATH:$LANGUAGE_MODELS_DIR/spacy:/nfs/raid66/u11/users/brozonoy-ad/modal_and_temporal_parsing";

my $CANONICAL_PAR_PATH = "$TEXT_OPEN_PYTHONPATH/config/canonical_pars";

return {
    language_input_entries            => [
        { lang => "en", par => "$CANONICAL_PAR_PATH/en.par", list => "/nfs/raid88/u10/users/hqiu_ad/data/canonical_corpus/en/file.list" },
        { lang => "en_full", par => "$CANONICAL_PAR_PATH/en_full.par", list => "/nfs/raid88/u10/users/hqiu_ad/data/canonical_corpus/en/file.list" },
        { lang => "ar", par => "$CANONICAL_PAR_PATH/ar.par", list => "/nfs/raid88/u10/users/hqiu_ad/data/canonical_corpus/ar/file.list" },
        { lang => "fa", par => "$CANONICAL_PAR_PATH/fa.par", list => "/nfs/raid88/u10/users/hqiu_ad/data/canonical_corpus/fa/file.list" },
        { lang => "zh_hans", par => "$CANONICAL_PAR_PATH/zh_hans.par", list => "/nfs/raid88/u10/users/hqiu_ad/data/canonical_corpus/zh_hans/file.list" }
    ],
    job_prefix                        => "canonical",
    use_gpus                          => 0,
    num_of_batches                    => 1,
    max_number_of_tokens_per_sentence => 128,
    max_jobs                          => 400,
    TEXT_OPEN_RELEASE_DIR             => $TEXT_OPEN_RELEASE_DIR,
    TEXT_OPEN_PYTHONPATH              => $TEXT_OPEN_PYTHONPATH,
    PYTHON_GPU                        => $PYTHON_GPU,
    PYTHON_CPU                        => $PYTHON_CPU,
    PYTHON3_SYSTEM                    => "/usr/bin/env python3",
    LANGUAGE_MODELS_DIR               => $LANGUAGE_MODELS_DIR,
    perl_libs                         => [
        "$RUNJOBS_RELEASE_DIR/lib",
        "$TEXT_OPEN_RELEASE_DIR/src/perl/text_open/lib",
    ],
    runjobs_pars                      => {
        batch_queue     => 'cpunodes-avx',
        batch_gpu_queue => 'gpu-12G', # It logically goes here but
        # isnt automatically used by runjobs
        queue_priority  => 5,
        local_dir       => "/export/u10",
        queue_mem_limit => [ '7.5G', '15.5G' ],
    },
}
