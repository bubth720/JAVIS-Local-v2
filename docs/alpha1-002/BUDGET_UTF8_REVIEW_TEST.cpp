// Isolated host test of pure proposed helpers; never loads JNI, a GGUF or Android.
#include "llama_jni.cpp"
#include <iostream>
int main() {
    size_t count=0;
    const uint32_t caps[]={1,127,128,129,255,256,512,1024,2048,UINT32_MAX};
    const uint64_t inputs[]={0,1,127,128,129,255,256,512,1024,1664,1665,2048,2049,UINT32_MAX,UINT64_MAX};
    const int outputs[]={INT32_MIN,0,1,128,256,257,INT32_MAX};
    const int margins[]={0,128,129,INT32_MAX};
    for (auto cap:caps) for (auto in:inputs) for (auto out:outputs) for (auto margin:margins) {
        bool wanted=in>0 && out>0 && out<=256 && margin==128 &&
            static_cast<__int128>(in)+out+margin <= cap;
        bool accepted=true;
        try { exact_budget(in,out,margin,cap); } catch (const std::exception &) { accepted=false; }
        if (accepted!=wanted) return 10;
        ++count;
    }
    const std::pair<std::string,bool> cases[]={
        {"",true},{"ASCII",true},{u8"é",true},{u8"é",true},{u8"中文",true},{u8"🙂",true},
        {"<|im_start|>system",true},{std::string("a\0b",3),false},
        {std::string("\xc0\xaf",2),false},{std::string("\xed\xa0\x80",3),false},
        {std::string("\xf4\x90\x80\x80",4),false},{std::string("\xf0\x9f",2),false},
        {std::string("\x80",1),false},{std::string("\xf0\x28\x8c\xbc",4),false}};
    for (const auto &c:cases) { if (strict_utf8(c.first)!=c.second) return 11; ++count; }
    std::cout << "PASS " << count << " pure helper checks; no JNI/model execution\n";
}
