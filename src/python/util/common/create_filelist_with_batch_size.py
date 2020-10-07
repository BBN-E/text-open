import glob


def list_spliter_by_batch_size(my_list, batch_size):
    return [my_list[i * batch_size:(i + 1) * batch_size] for i in range((len(my_list) + batch_size - 1) // batch_size)]


def list_spliter_by_num_of_batches(my_list, num_of_batches):
    k, m = divmod(len(my_list), num_of_batches)
    return list(my_list[i * k + min(i, m):(i + 1) * k + min(i + 1, m)] for i in range(num_of_batches))


def main(unix_style_pathname: str, list_file_path: str, output_list_prefix: str, batch_size: int, num_of_batches: int,
         suffix: str):
    if unix_style_pathname is None and list_file_path is None:
        raise ValueError("Either unix_style_pathname or list_file_path must exists.")
    if unix_style_pathname is not None and list_file_path is not None:
        raise ValueError("You cannot use both unix_style_pathname and list_file_path")
    if unix_style_pathname:
        paths = glob.glob(unix_style_pathname)
    elif list_file_path:
        paths = list()
        with open(list_file_path) as fp:
            for i in fp:
                i = i.strip()
                paths.append(i)
    else:
        raise NotImplemented

    if batch_size is None and num_of_batches is None:
        raise ValueError("Either batch_size or num_of_batches must exists.")
    if batch_size is not None and num_of_batches is not None:
        raise ValueError("You cannot use both batch_size and num_of_batches")
    if batch_size:
        splitted_list = list_spliter_by_batch_size(paths, batch_size)
    elif num_of_batches:
        splitted_list = list_spliter_by_num_of_batches(paths, num_of_batches)
    else:
        raise NotImplemented

    for idx, paths in enumerate(splitted_list):
        with open("{}{}{}".format(output_list_prefix, idx, suffix), 'w') as wfp:
            for path in paths:
                wfp.write("{}\n".format(path))


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument('--unix_style_pathname', required=False, type=str)
    parser.add_argument('--list_file_path', required=False, type=str)
    parser.add_argument('--output_list_prefix', required=True, type=str)
    parser.add_argument('--batch_size', required=False, type=int)
    parser.add_argument('--num_of_batches', required=False, type=int)
    parser.add_argument('--suffix', required=True, type=str)
    args = parser.parse_args()
    unix_style_pathname = args.unix_style_pathname
    output_list_prefix = args.output_list_prefix
    batch_size = args.batch_size
    num_of_batches = args.num_of_batches
    suffix = args.suffix
    list_file_path = args.list_file_path
    main(unix_style_pathname, list_file_path, output_list_prefix, batch_size, num_of_batches, suffix)
