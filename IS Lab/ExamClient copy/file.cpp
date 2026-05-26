// Source - https://stackoverflow.com/q
// Posted by QKL, modified by community. See post 'Timeline' for change history
// Retrieved 2026-01-28, License - CC BY-SA 3.0

#include <stdio.h>

char[] rotate(char c[]) {
  char single;
  int i;
  int alen = sizeof(c)/sizeof(c[0]);
  char out[alen];

  for(i=0;i<=alen;i+=1) {
    if(c[i]>='a' && (c[i]+13)<='z'){
      out[i] = c[i]+13;
    }
  }

  return out;
}

int main(int argc, char *argv[]) {
  printf("The given args will be rotated\n");
  int i;
  char rotated[sizeof(argv)/sizeof(argv[0])];

  rotated = rotate(argv);

  /* printing rotated[] later on */
  return 0;

}
