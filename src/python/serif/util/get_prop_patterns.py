import sys, os
script_dir = os.path.dirname(__file__)
sys.path.append(os.path.join(script_dir, "..", ".."))
import serifxml3
from serifxml3 import PredType
from serifxml3 import MentionType 

def get_event_mention_maps(sentence, list_of_event_mentions):
    # Results caches
    event_mention_to_object_map = dict()
    object_to_event_mention_map = dict()

    # Map mention heads to mentions, so we don't have to 
    # iterate over the mentions for every event mention
    mention_map = dict()
    for m in sentence.mention_set:
        if m.mention_type == MentionType.none:
            continue
        mention_head = m.syn_node.head_preterminal
        if mention_head not in mention_map:
            mention_map[mention_head] = set()
        #print("Mapping " + mention_head.pprint() + " to " + m.pprint())
        mention_map[mention_head].add(m)

    # Align event mentions with event mentions
    for em in list_of_event_mentions:
        event_head = None
        if em.anchor_node.is_terminal:
            event_head = em.anchor_node.parent
        else:
            event_head = em.anchor_node.head_preterminal
        #print ("Looking for mention match for: " + event_head.pprint())
        if event_head in mention_map:
            for m in mention_map[event_head]:
                #print ("Matched: " + m.pprint())
                if em not in event_mention_to_object_map:
                    event_mention_to_object_map[em] = set()
                event_mention_to_object_map[em].add(m)
                if m not in object_to_event_mention_map:
                    object_to_event_mention_map[m] = set()
                object_to_event_mention_map[m].add(em)

    # Map prop heads to props, so we don't have to 
    # iterate over the props for every event mention
    prop_map = dict()
    for p in sentence.proposition_set:
        if p.head is None:
            continue
        prop_head = p.head.head_preterminal
        #print ("Mapping " + prop_head.pprint() + " to " + p.pprint())
        if prop_head not in prop_map:
            prop_map[prop_head] = set()
        prop_map[prop_head].add(p)

    # Align propositions to with event mentions
    for em in list_of_event_mentions:
        event_head = None
        if em.anchor_node.is_terminal:
            event_head = em.anchor_node.parent
        else:
            event_head = em.anchor_node.head_preterminal   
        #print ("Looking for prop match for: " + event_head.pprint())
        if event_head in prop_map:
            for p in prop_map[event_head]:
                #print ("Matched: " + p.pprint())
                if em not in event_mention_to_object_map:
                    event_mention_to_object_map[em] = set()
                event_mention_to_object_map[em].add(p)
                if p not in object_to_event_mention_map:
                    object_to_event_mention_map[p] = set()
                object_to_event_mention_map[p].add(em)
        
    return event_mention_to_object_map, object_to_event_mention_map

def get_prop_map(sentence, object_to_event_mention_map):
    results = dict()
    starting_prop_to_depth = dict()
    for p in sentence.proposition_set:
        results[p] = set()
        starting_prop_to_depth[p] = 0
        #print ("Top level for " + p.pprint() + "\n")
        traverse(p, p, object_to_event_mention_map, 0, results, 
                 starting_prop_to_depth)
    return results, starting_prop_to_depth

def traverse(starting_prop, reachable_prop, 
             object_to_event_mention_map, depth, results, 
             starting_prop_to_depth):
    if reachable_prop in object_to_event_mention_map:
        #print ("PROP " + str(depth))
        add_all(results[starting_prop], 
                object_to_event_mention_map[reachable_prop])
        if depth > starting_prop_to_depth[starting_prop]:
             starting_prop_to_depth[starting_prop] = depth

    for a in reachable_prop.arguments:
        if a.proposition is not None:
            traverse(starting_prop, a.proposition, 
                     object_to_event_mention_map, depth + 1,
                     results, starting_prop_to_depth)
        if a.mention is not None:
            if a.mention in object_to_event_mention_map:
                #print ("MENTION " + str(depth+1))
                add_all(results[starting_prop], 
                        object_to_event_mention_map[a.mention])
                if depth + 1 > starting_prop_to_depth[starting_prop]:
                    starting_prop_to_depth[starting_prop] = depth + 1

def add_all(s, lst):
    #print ("Found these event mentions:")
    for em in lst:
        #print(em.anchor_node.pprint())
        s.add(em)

def get_best_proposition(prop_list, prop_to_depth):
    min_depth = None
    shallowest_prop = None
    for p in prop_list:
        if shallowest_prop is None:
            shallowest_prop = p
            min_depth = prop_to_depth[p]
            continue
        if prop_to_depth[p] < min_depth:
            shallowest_prop = p
            min_depth = prop_to_depth[p]
            continue
    #print ("Depth: " + str(min_depth))
    return shallowest_prop

def get_prop_patterns_between_pairs(sentence, list_of_event_mentions):
    results = dict()

    # EventMention to list of Propositions and Mentions whose head
    # overlaps with EventMention trigger
    event_mention_to_object_map, object_to_event_mention_map\
        = get_event_mention_maps(sentence, list_of_event_mentions)

    # "Starting" Proposition to set of EventMentions
    # that can be reached by walking down from the starting
    # Proposition.
    starting_prop_to_child_map, prop_to_depth = get_prop_map(
        sentence, object_to_event_mention_map)
    
    for i in range(len(list_of_event_mentions)):
        for j in range(i + 1, len(list_of_event_mentions)):
            em1 = list_of_event_mentions[i]
            em2 = list_of_event_mentions[j]

            candidate_props = []
            for (starting_prop, reachable_event_mentions) in\
                    starting_prop_to_child_map.items():
                if (em1 in reachable_event_mentions and 
                    em2 in reachable_event_mentions):
                    candidate_props.append(starting_prop)

            if len(candidate_props) > 0:
                best_prop = get_best_proposition(candidate_props, prop_to_depth)
                results[(em1, em2)] = best_prop
            #else:
            #    print("This pair has no connection: " + 
            #          em1.anchor_node.pprint() + " " + 
            #          em2.anchor_node.pprint() + "\n")
        
    return results

def get_prop_pattern(prop):
    pattern_string = "("
    if prop.pred_type == PredType.verb:
        pattern_string += "vprop"
    elif prop.pred_type == PredType.copula:
        pattern_string += "vprop"
    elif prop.pred_type == PredType.modifier:
        pattern_string += "mprop"
    elif prop.pred_type == PredType.noun:
        pattern_string += "nprop"
    elif prop.pred_type == PredType.set:
        pattern_string += "sprop"
    elif prop.pred_type == PredType.comp:
        pattern_string += "cprop"
    else:
        pattern_string += "prop"

    if prop.head:
        pattern_string += " (predicate " + prop.head.head_terminal.tag + ")"
    
    if len(prop.arguments) > 0:
        pattern_string += " (args"
        for arg in prop.arguments:
            pattern_string += " (argument (role " + arg.role + ")"
            if arg.mention is not None:
                pattern_string += " " + get_mention_pattern(arg.mention)
            elif arg.proposition is not None:
                pattern_string += " " + get_prop_pattern(arg.proposition)
            pattern_string += ")"
        pattern_string += ")" # closes args

    pattern_string += ")"    

    return pattern_string 

def get_mention_pattern(mention):
    pattern_string = "(mention"
    pattern_string += " (headword " + mention.head.head_terminal.tag + ")"

    pattern_string += ")"

    return pattern_string

if __name__ == "__main__":
    doc = serifxml3.Document(
        "/nfs/raid87/u11/users/azamania/runjobs/expts/Hume/causeex_sams_1212a_baseline/final_serifxml/cb348c035d79f5600fe14fa7e2233051/ENG_NW_CX_SAMS_Expt_1_cb348c035d79f5600fe14fa7e2233051_0.xml")

    for sentence in doc.sentences:
        for event_pair, prop in get_prop_patterns_between_pairs(
            sentence, sentence.event_mention_set).items():
            
            print("\nEvent pair: " + event_pair[0].anchor_node.pprint() +
                  " " + event_pair[1].anchor_node.pprint())
            print (get_prop_pattern(prop))
