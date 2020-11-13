import os

def top_images(root_dir):
    children_dir = os.walk(root_dir)
    all_png = []
    for r, dirs, files in children_dir:
        for f in [x for x in files if can_convert(x)]:
            all_png.append(f)
    return all_png

def blog_images(root_dir):
    children_dir = os.walk(root_dir)
    all_blog = []
    for r, dirs, files in children_dir:
        for d in dirs:
            all_blog.append(d)
    return all_blog        

def convert_to_webp(blog_dir):
    blog_file = blog_dir + '.md' 
    blog_dir = os.path.join('_posts', blog_dir)
    #os.system('webpc -q 75 -o ' + blog_dir + ' ' + blog_dir)
    for i in os.listdir(blog_dir):
        if can_convert(i):
            filename = os.path.splitext(os.path.basename(i))[0] + '.webp'
            os.system('cwebp -q 75 ' +  os.path.join(blog_dir, i) + ' -o ' + os.path.join(blog_dir, filename))
    replace_old_md(os.path.join('_posts', blog_file))

def replace_old_md(blog_file):
    f = file(blog_file, 'r')
    old_content = f.readlines()
    new_content = []
    for line in old_content:
        if line.startswith('!['): 
            if '.png)' in line:
                new_content.append(line.replace('.png)', '.webp)')) 
            elif '.PNG)' in line:
                new_content.append(line.replace('.PNG)', '.webp)')) 
            elif '.jpg)' in line:
                new_content.append(line.replace('.jpg)', '.webp)')) 
            elif '.JPG)' in line:
                new_content.append(line.replace('.JPG)', '.webp)')) 
            else:
                new_content.append(line)
        else:
            new_content.append(line)
    f.close()
    f = file(blog_file, 'w')
    f.writelines(new_content)
    f.close()
    



def is_img_tag(line):
    return '.png' in line or '.PNG' in line or '.jpg' in line or '.JPG' in line
    
def can_convert(filename):
    return filename.endswith('.png') or filename.endswith('.jpg') or filename.endswith('.JPG') or filename.endswith('.PNG')

if __name__ == '__main__':
    #for i in top_images('images'):
    #    print i
    for i in blog_images('_posts'):
        convert_to_webp(i)
        #print i
