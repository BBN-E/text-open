import serifxml3

granular_types = {
    "Corruplate",
    "Epidemiplate",
    "Protestplate",
    "Terrorplate",
    "Disasterplate",
    "Displacementplate"
}

def serifxml_dumper(serifxml_path):
    role_to_cnt = dict()
    serif_doc = serifxml3.Document(serifxml_path)
    for serif_event in serif_doc.event_set or ():
        if serif_event.event_type in granular_types:
            for argument in serif_event.arguments or ():
                role = argument.role
                role_to_cnt[role] = role_to_cnt.get(role,0) + 1
    return role_to_cnt

def main():
    input_serifxml_list = "/nfs/raid88/u10/users/ychan-ad/BETTER/p2_granular_events/data/serifxml/train_p2/output.list"
    role_to_cnt = dict()
    with open(input_serifxml_list) as fp:
        for i in fp:
            i = i.strip()
            role_to_cnt_local = serifxml_dumper(i)
            for role, cnt in role_to_cnt_local.items():
                role_to_cnt[role] = role_to_cnt.get(role, 0) + cnt
    for role, cnt in sorted(role_to_cnt.items(), key=lambda x:x[1], reverse=True):
        # print("\"{}\" : \"{}\",".format(role, role))
        print("\"{}\",".format(role))

if __name__ == "__main__":
    main()