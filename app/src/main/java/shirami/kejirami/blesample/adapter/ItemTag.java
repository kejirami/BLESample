package shirami.kejirami.blesample.adapter;

import androidx.annotation.Nullable;

public class ItemTag implements Comparable<ItemTag> {
    private String mId;

    public ItemTag(String id){
        mId = id;
    }

    public String getId(){
        return mId;
    }
    public void setId(String id){
        mId = id;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof ItemTag) return mId.equals(((ItemTag) obj).getId());
        return false;
    }

    @Override
    public int compareTo(ItemTag itemTag) {
        return mId.compareTo(itemTag.getId());
    }
}
